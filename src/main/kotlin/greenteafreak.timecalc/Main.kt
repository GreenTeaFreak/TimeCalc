package greenteafreak.timecalc

import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.*
import java.lang.StringBuilder

const val doLog = false

inline fun debug(log : () -> String?) {
    if (doLog) println(log())
}

fun Long.toHMS() : Triple<Long, Long, Long> {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s =(this % 3600) % 60
    return Triple(h, m, s)
}

fun Triple<Long, Long, Long>.asTimeStr() =
    "%02d:%02d.%02d".format(first, second, third)

fun main() {
    println("enter: expressions, 'help' or 'exit'")
    val bf = StringBuilder(256)

    while(true) {
        var line = readLine()
        if ("exit" == line) break

        if ("help" == line) {
            printHelp()
            continue
        }

        if (line.isNullOrBlank() && bf.isNotEmpty()) {
            bf.toString().calc()
            bf.clear()
            continue
        }

        bf.append(line ?: "")
    }
}

private fun printHelp() {
    println("""
        
            Enter Expression, end with CTRL-C or type 'exit':"
              - valid formats: h:m(.s)?, 1h, 1m, 1s
                - suported operations: '+', '-', Timestamp '*|/ <factor>'
                
        """.trimIndent())
}

private fun String.calc() {
    val cs = CharStreams.fromString(this) //  + 30m + 02:25.33
    val ls = TimeLexer(cs)
    val ts = CommonTokenStream(ls)
    val pa = TimeParser(ts)
    val vi = MyVisitor()

    pa.errorHandler = BailErrorStrategy()
    val result = vi.visit(pa.root())
    println(result.toHMS().asTimeStr())
    println()
}

class MyVisitor() : TimeBaseVisitor<Long>() {

    override fun visitRoot(ctx: TimeParser.RootContext?): Long {
        debug { "visitRoot: " + ctx!!.text }
        val r =  visit(ctx!!.expr())
        return r
    }

    override fun visitBrkexpr(ctx: TimeParser.BrkexprContext?): Long {
        debug { "VISIT BREAK:" + ctx!!.text }
        val brkVal =  visit(ctx!!.expr())
        debug { "BRAKE VALUE=" + brkVal }
        return brkVal
    }

    override fun visitExpr(ctx: TimeParser.ExprContext?): Long {
        debug {"visitExpr: " + ctx!!.text }

        if (ctx!!.timeval() != null) {
            return visit(ctx.timeval())
        }

        if (ctx.brkexpr() != null) {
                return visit(ctx.brkexpr())
        }

        if (ctx.TIMES() != null) {
            return ctx.DIGIT().single().asLong() * visit(ctx.expr())
        }

        if (ctx.DIVIDE() != null) {
            return visit(ctx.expr()) / ctx.DIGIT().single().asLong()
        }

        // operation
        return visit(ctx.expr()) + visit(ctx.operation())
    }

    override fun visitOperation(ctx: TimeParser.OperationContext?): Long {
        debug {"visitOperation: " + ctx!!.text }
        val mult = when {
            ctx!!.MINUS() != null -> -1
            ctx.PLUS() != null -> 1
            else -> error("unknown operation")
        }

        return mult * visit(ctx.expr())
    }

    override fun visitTimeval(ctx: TimeParser.TimevalContext?): Long {
        debug {"visitTimeval: " + ctx!!.text }
        checkNotNull(ctx)

        if (ctx.hms() != null) {
            val r = ctx.hms().let {
                it.DIGIT(0).asLong(60*60) +
                it.DIGIT(1).asLong(60) +
                it.DIGIT(2).asLong()
            }

            debug {"CALC HMS=$r" }
            return r
        }

        val r =  when {
            ctx.hour() != null -> ctx.hour().DIGIT().first().asLong(60 * 60)
            ctx.minute() != null -> ctx.minute().DIGIT().first().asLong(60)
            ctx.second() != null -> ctx.second().DIGIT().first().asLong()
            else -> error("unmatched time token")
        }

        debug { "calc single=$r" }
        return r
    }

    fun TerminalNode?.asLong(mult : Long = 1) : Long =
        if (this == null) 0 else this.text.toLong() * mult
}
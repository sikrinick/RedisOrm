package com.sikri.redis_orm.utils


class IndentedPrinter(indent: Int = 0): Appendable {
    private val indent = if (indent > 0) " ".repeat(indent) else ""

    override fun append(csq: CharSequence?): Appendable {
        return System.out.append(indent).append(csq)
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): Appendable {
        return System.out.append(indent).append(csq, start ,end)
    }

    override fun append(c: Char): Appendable {
        return System.out.append(indent).append(c)
    }

}
package org.liamjd.bascule.render

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import java.util.*

class Paginate : Helper<Any> {
	override fun apply(context: Any?, options: Options): Any {
		val currentPage = options.params[0] as Int
		val totalPages = options.params[1] as Int
		val buffer = options.buffer()
		val fn = options.fn

		if(context is Iterable<*>) {
			val pagination = context.iterator()
			val parent = options.context
			var index = 1
			while (pagination.hasNext()) {
				val pg = pagination.next() as String
				val pgCtx = Context.newContext(parent,pg)
				val pgInt = try { pg.toInt() } catch (nfe: NumberFormatException) {
					-1
				}

				pgCtx.combine("@first",if(1.toString() == pg) "first" else "")
						.combine("@index",index)
						.combine("@last",if(pg==totalPages.toString()) "last" else "")
						.combine("@current",if(pg=="*") "current" else "")
						.combine("@ellipsis",if(pg==".") "ellipsis" else "")
						.combine("@page",if(pgInt != -1) "$pgInt" else "")

				buffer.append(options.apply(fn, pgCtx, Arrays.asList<Any>(pg, index)))
				index++
			}

			return buffer as Any

		} else {
			return "context is not iterable with params $context $currentPage $totalPages "
		}
	}
}

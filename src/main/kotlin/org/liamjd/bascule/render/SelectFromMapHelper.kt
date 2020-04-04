package org.liamjd.bascule.render

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import java.util.*

/**
 * A helper to select a specific named item from a map
 * Specify the key and it will return the result under the label @item
 */
class SelectFromMapHelper : Helper<Any> {
	override fun apply(context: Any?, options: Options?): Any? {
		if(context is Map<*,*>) {
			val choice: String
			if(options == null) {
				return null
			}
			if(options.params.isNotEmpty()) {
				choice = options.param<String>(0)
				val item = context[choice] as Any
				val buffer = options.buffer()

				val loop = context.iterator()
				val base: Int = options.hash("base", 0) ?: 0
				var index = base
				var even = index % 2 == 0
				val parent = options.context
				val fn = options.fn
				var limitCounter = 0
				while (loop.hasNext()) {
					val it = loop.next()
					val itCtx = Context.newContext(parent, it)
					itCtx.combine("@item",item)
					buffer?.append(options.apply(fn, itCtx, Arrays.asList<Any>(it, index)))
					index += 1
					even = !even
					limitCounter++
				}
				return buffer as Any
			} else {
				return null
			}

		}
		// shouldn't get here!
		return emptyList<Any>()
	}
}

package org.liamjd.bascule.pipeline

import org.liamjd.bascule.generator.Post

interface Pipeline {


	fun process()
}

class IndexPipeline(val posts: List<Post>) : Pipeline {

	override fun process() {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}

package org.liamjd.bascule

@Deprecated("Test only", ReplaceWith("none"), DeprecationLevel.HIDDEN)
fun main(args: Array<String>) {

		for (totalPages in 1..12) {
		println("Total Pages: $totalPages")

		for (currentPage in 1..totalPages) {

			val prev = currentPage - 1
			val next = currentPage + 1
			val isFirst = (currentPage == 1)
			val isLast = (currentPage == totalPages)
			val prevIsFirst = (currentPage - 1 == 1)
			val nextIsLast = (currentPage + 1 == totalPages)

			if (totalPages == 1) {
				print("*[1]*")
			} else if (isFirst) {
				print("*[$currentPage]* ")

				if (nextIsLast) {
					print("[$next]")
				} else {
					print("[$next]")
					print("...")
					print("[$totalPages]")
				}
			} else if (prevIsFirst) {
				print("[1] ")
				print("*[$currentPage]* ")
				if (!isLast) {
					print("... ")
					print("[$totalPages]")
				}
			} else if (!isLast) {
				print("[1] ")
				print("...")
				print("[$prev] ")
				print("*[$currentPage]* ")
				if (!nextIsLast) {
					print("[$next] ")
					print("... ")
					print("[$totalPages]")
				} else {
					print("[$totalPages]")
				}
			} else {
				print("")
			}
			println()
		}
		println("-------------------------")
	}
}

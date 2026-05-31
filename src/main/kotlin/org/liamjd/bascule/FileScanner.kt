package org.liamjd.bascule

import java.io.File

/**
 * A narrow, local abstraction over the disk-touching filesystem operations the scanner needs:
 * directory listing and file metadata (size / modification time / directory test).
 *
 * It is defined here in bascule-static (rather than added to bascule-lib's `FileHandler`) so that the
 * change-set calculation can be unit tested with an in-memory fake, without first cutting a bascule-lib
 * release. Path-only operations (`File.name`, `.extension`, `.parentFile`, `.absolutePath`) are pure
 * string manipulation and deliberately stay as plain `File` calls.
 */
interface FileScanner {

	/** The immediate children of [folder], or an empty list if it is not a readable directory. */
	fun listFiles(folder: File): List<File>

	fun isDirectory(file: File): Boolean

	/** File length in bytes. */
	fun length(file: File): Long

	/** Last-modified time in epoch milliseconds. */
	fun lastModified(file: File): Long
}

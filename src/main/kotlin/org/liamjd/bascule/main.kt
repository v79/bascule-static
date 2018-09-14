package org.liamjd.bascule

import picocli.CommandLine

/**
 * Launches the Bascule generator, passing command line arguments
 */
fun main(args: Array<String>) = CommandLine.run(Bascule(), *args)
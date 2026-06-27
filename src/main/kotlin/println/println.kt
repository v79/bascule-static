package println

var reporter: Reporter = MordantReporter()

fun info(msg: String) = reporter.info(msg)
fun error(msg: String) = reporter.error(msg)
fun warn(msg: String) = reporter.warn(msg)
fun debug(msg: String) = reporter.debug(msg)
fun progress(label: String, current: Int, message: String? = null) = reporter.progress(label, current, message)
fun clearProgress() = reporter.clearProgress()


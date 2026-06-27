package println

/**
 * Interface for reporting progress and errors to the user
 */
interface Reporter {
    var verbose: Boolean
    fun info(msg: String)
    fun error(msg: String)
    fun warn(msg: String)
    fun debug(msg: String)
    fun progress(label: String, current: Int, message: String? = null)
    fun clearProgress()
}
package jake2.qcommon

import jake2.Defines

/**
 * Created by tropper on 28.05.17.
 */
class CommandLineOptions
/**
 * Com_InitArgv checks the number of command line arguments
 * and copies all arguments with valid length into values.
 */
@Throws(QuakeException::class)
constructor(args: Array<String>) {

    internal val count: Int
    internal val values: Array<String>

    init {

        if (args.size > MAX_NUM_ARGVS) {
            Command.Error(Defines.ERR_FATAL, "count > MAX_NUM_ARGVS")
        }

        values = args.copyOf()
        count = args.size
        for (i in 0..count - 1) {
            if (args[i].length >= Defines.MAX_TOKEN_CHARS)
                values[i] = ""
            else
                values[i] = args[i]
        }
    }

    fun count(): Int {
        return count
    }

    fun valueAt(pos: Int): String {
        if (pos < 0 || pos >= count || values[pos].length < 1)
            return ""
        return values[pos]
    }

    fun clearValueAt(pos: Int) {
        if (pos < 0 || pos >= count || values[pos].length < 1)
            return
        values[pos] = ""
    }

    companion object {

        private val MAX_NUM_ARGVS = 50
    }
}

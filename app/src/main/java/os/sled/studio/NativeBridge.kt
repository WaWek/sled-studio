package os.sled.studio

object NativeBridge {
    init { System.loadLibrary("sled_core") }
    external fun nativeVersion(): Long
    external fun nativeBfmmlaTest(): Long
}
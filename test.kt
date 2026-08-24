import androidx.media3.exoplayer.DefaultRenderersFactory
fun test() {
    DefaultRenderersFactory(null).setEnableDecoderFallback(true)
}

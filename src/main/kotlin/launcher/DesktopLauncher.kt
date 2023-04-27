// Adapted from Unciv, the civ-V clone, by yairm210

package launcher

import QuantumWerewolfGame
import UI.images.ImagePacker
import UI.saving.CustomFileLocationHelper
import UI.saving.json
import UI.util.*
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.HdpiMode
import java.awt.*
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JFrame


internal object DesktopLauncher {

    @JvmStatic
    fun main(arg: Array<String>) {
        // Solves a rendering problem in specific GPUs and drivers.
        // For more info see https://github.com/yairm210/Unciv/pull/3202 and https://github.com/LWJGL/lwjgl/issues/119
        System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true")
        // This setting (default 64) limits clipboard transfers. Value in kB!
        // 386 is an almost-arbitrary choice from the saves I had at the moment and their GZipped size.
        // There must be a reason for lwjgl3 being so stingy, which for me meant to stay conservative.
        System.setProperty("org.lwjgl.system.stackSize", "384")

        val isRunFromJAR = DesktopLauncher.javaClass.`package`.specificationVersion != null
        ImagePacker.packImages(isRunFromJAR)

        val config = Lwjgl3ApplicationConfiguration()
        config.setWindowIcon(Files.FileType.Internal, "ExtraImages/icon.png")
        config.setTitle("Quantum Werewolves")
        config.setHdpiMode(HdpiMode.Logical)
        config.setWindowSizeLimits(120, 80, -1, -1)

        // We don't need the initial Audio created in Lwjgl3Application, HardenGdxAudio will replace it anyway.
        // Note that means config.setAudioConfig() would be ignored too, those would need to go into the HardenedGdxAudio constructor.
        config.disableAudio(true)

        config.setWindowedMode(120, 80)

        val settings = QuantumFiles.getSettingsForPlatformLaunchers()
        if (settings.isFreshlyCreated) {
            settings.screenSize = ScreenSize.Enormous // By default we guess that Desktops have larger screens
            // LibGDX not yet configured, use regular java class
            val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val maximumWindowBounds = graphicsEnvironment.maximumWindowBounds
            println("Max Window Bounds: $maximumWindowBounds")
            settings.windowState = WindowState(
                width = maximumWindowBounds.width,
                height = maximumWindowBounds.height
            )
            FileHandle(SETTINGS_FILE_NAME).writeString(json().toJson(settings), false) // so when we later open the game we get fullscreen
        }

        config.setWindowedMode(settings.windowState.width.coerceAtLeast(120), settings.windowState.height.coerceAtLeast(80))

        val desktopParameters = QWParameters(
            fontImplementation = NativeFontDesktop((Fonts.ORIGINAL_FONT_SIZE * settings.fontSizeMultiplier).toInt(), settings.fontFamily),
            customFileLocationHelper = CustomFileLocationHelperDesktop(),
        )

        val game = QuantumWerewolfGame(desktopParameters)

        Lwjgl3Application(game, config)
    }
}


class NativeFontDesktop(private val size: Int, private val fontFamily: String) :
    NativeFontImplementation {
    private val font by lazy {
        Font(fontFamily, Font.PLAIN, size)
    }
    private val metric by lazy {
        val bi = BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR)
        val g = bi.createGraphics()
        g.font = font
        val fontMetrics = g.fontMetrics
        g.dispose()
        fontMetrics
    }

    override fun getFontSize(): Int {
        return size
    }

    override fun getCharPixmap(char: Char): Pixmap {
        var width = metric.charWidth(char)
        var height = metric.ascent + metric.descent
        if (width == 0) {
            height = size
            width = height
        }
        val bi = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
        val g = bi.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.font = font
        g.color = Color.WHITE
        g.drawString(char.toString(), 0, metric.ascent)
        val pixmap = Pixmap(bi.width, bi.height, Pixmap.Format.RGBA8888)
        val data = bi.getRGB(0, 0, bi.width, bi.height, null, 0, bi.width)
        for (i in 0 until bi.width) {
            for (j in 0 until bi.height) {
                pixmap.setColor(Integer.reverseBytes(data[i + (j * bi.width)]))
                pixmap.drawPixel(i, j)
            }
        }
        g.dispose()
        return pixmap
    }

    override fun getAvailableFontFamilies(): Sequence<FontFamilyData> {
        val cjkLanguage = " CJK " +System.getProperty("user.language").uppercase()
        return GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts.asSequence()
            .filter { " CJK " !in it.fontName || cjkLanguage in it.fontName }
            .map { FontFamilyData(it.family, it.getFamily(Locale.ROOT)) }
            .distinctBy { it.invariantName }
    }
}


class CustomFileLocationHelperDesktop : CustomFileLocationHelper() {

    override fun createOutputStream(suggestedLocation: String, callback: (String?, OutputStream?, Exception?) -> Unit) {
        pickFile(callback, JFileChooser::showSaveDialog, File::outputStream, suggestedLocation)
    }

    override fun createInputStream(callback: (String?, InputStream?, Exception?) -> Unit) {
        pickFile(callback, JFileChooser::showOpenDialog, File::inputStream)
    }

    private fun <T> pickFile(callback: (String?, T?, Exception?) -> Unit,
                             chooseAction: (JFileChooser, Component) -> Int,
                             createValue: (File) -> T,
                             suggestedLocation: String? = null) {
        EventQueue.invokeLater {
            try {
                val fileChooser = JFileChooser().apply fileChooser@{
                    if (suggestedLocation == null) {
                        currentDirectory = Gdx.files.local("").file()
                    } else {
                        selectedFile = File(suggestedLocation)
                    }
                }

                val result: Int
                val frame = JFrame().apply frame@{
                    setLocationRelativeTo(null)
                    isVisible = true
                    toFront()
                    result = chooseAction(fileChooser, this@frame)
                    dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
                }

                frame.dispose()

                if (result == JFileChooser.CANCEL_OPTION) {
                    callback(null, null, null)
                } else {
                    val value = createValue(fileChooser.selectedFile)
                    callback(fileChooser.selectedFile.absolutePath, value, null)
                }
            } catch (ex: Exception) {
                callback(null, null, ex)
            }
        }
    }
}

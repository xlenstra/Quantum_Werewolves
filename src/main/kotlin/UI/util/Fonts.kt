package UI.util
// Adapted from Unciv, the civ-V clone, by yairm210

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable

interface NativeFontImplementation {
    fun getFontSize(): Int
    fun getCharPixmap(char: Char): Pixmap
    fun getAvailableFontFamilies(): Sequence<FontFamilyData>
}

// If save in `GameSettings` need use invariantFamily.
// If show to user need use localName.
// If save localName in `GameSettings` may generate garbled characters by encoding.
class FontFamilyData(
    val localName: String,
    val invariantName: String = localName
) {
    // Implement kotlin equality contract such that _only_ the invariantName field is compared.
    override fun equals(other: Any?): Boolean {
        return if (other is FontFamilyData) invariantName == other.invariantName
        else super.equals(other)
    }

    override fun hashCode() = invariantName.hashCode()

    /** For SelectBox usage */
    override fun toString() = localName

    companion object {
        val default = FontFamilyData("Default Font", Fonts.DEFAULT_FONT_FAMILY)
    }
}

// This class is loosely based on libgdx's FreeTypeBitmapFontData
class NativeBitmapFontData(
    private val fontImplementation: NativeFontImplementation
) : BitmapFontData(), Disposable {

    val regions: Array<TextureRegion>

    private var dirty = false
    private val packer: PixmapPacker

    private val filter = Texture.TextureFilter.Linear

    init {
        // set general font data
        flipped = false
        lineHeight = fontImplementation.getFontSize().toFloat()
        capHeight = lineHeight
        ascent = -lineHeight
        down = -lineHeight

        // Create a packer.
        val size = 1024
        val packStrategy = PixmapPacker.GuillotineStrategy()
        packer = PixmapPacker(size, size, Pixmap.Format.RGBA8888, 1, false, packStrategy)
        packer.transparentColor = Color.WHITE
        packer.transparentColor.a = 0f

        // Generate texture regions.
        regions = Array()
        packer.updateTextureRegions(regions, filter, filter, false)

        // Set space glyph.
        val spaceGlyph = getGlyph(' ')
        spaceXadvance = spaceGlyph.xadvance.toFloat()
    }

    override fun getGlyph(ch: Char): Glyph {
        var glyph: Glyph? = super.getGlyph(ch)
        if (glyph == null) {
            val charPixmap = getPixmapFromChar(ch)

            glyph = Glyph()
            glyph.id = ch.code
            glyph.width = charPixmap.width
            glyph.height = charPixmap.height
            glyph.xadvance = glyph.width

            val rect = packer.pack(charPixmap)
            charPixmap.dispose()
            glyph.page = packer.pages.size - 1 // Glyph is always packed into the last page for now.
            glyph.srcX = rect.x.toInt()
            glyph.srcY = rect.y.toInt()

            // If a page was added, create a new texture region for the incrementally added glyph.
            if (regions.size <= glyph.page)
                packer.updateTextureRegions(regions, filter, filter, false)

            setGlyphRegion(glyph, regions.get(glyph.page))
            setGlyph(ch.code, glyph)
            dirty = true
        }
        return glyph
    }

    private fun getPixmapFromChar(ch: Char): Pixmap {
        // Images must be 50*50px so they're rendered at the same height as the text - see Fonts.ORIGINAL_FONT_SIZE
        return fontImplementation.getCharPixmap(ch)
    }

    override fun getGlyphs(run: GlyphLayout.GlyphRun, str: CharSequence, start: Int, end: Int, lastGlyph: Glyph?) {
        packer.packToTexture = true // All glyphs added after this are packed directly to the texture.
        super.getGlyphs(run, str, start, end, lastGlyph)
        if (dirty) {
            dirty = false
            packer.updateTextureRegions(regions, filter, filter, false)
        }
    }

    override fun dispose() {
        packer.dispose()
    }

}

object Fonts {
    
    /** All text is originally rendered in 50px (set in AndroidLauncher and DesktopLauncher), and then scaled to fit the size of the text we need now.
     * This has several advantages: It means we only render each character once (good for both runtime and RAM),
     * AND it means that our 'custom' emojis only need to be once size (50px) and they'll be rescaled for what's needed. */
    const val ORIGINAL_FONT_SIZE = 50f
    const val DEFAULT_FONT_FAMILY = ""

    lateinit var font: BitmapFont

    /** This resets all cached font data in object Fonts.
     *  Do not call from normal code - reset the Skin instead: `BaseScreen.setSkin()`
     */
    fun resetFont() {
        val fontData = NativeBitmapFontData(QuantumWerewolfGame.Current.fontImplementation!!)
        font = BitmapFont(fontData, fontData.regions, false)
        font.setOwnsTexture(true)
        font.data.setScale(Constants.defaultFontSize / ORIGINAL_FONT_SIZE)
    }

    /** This resets all cached font data and allows changing the font */
    fun resetFont(newFamily: String) {
        try {
            val fontImplementationClass = QuantumWerewolfGame.Current.fontImplementation!!::class.java
            val fontImplementationConstructor = fontImplementationClass.constructors.first()
            val newFontImpl = fontImplementationConstructor.newInstance((ORIGINAL_FONT_SIZE * QuantumWerewolfGame.Current.settings.fontSizeMultiplier).toInt(), newFamily)
            if (newFontImpl is NativeFontImplementation)
                QuantumWerewolfGame.Current.fontImplementation = newFontImpl
        } catch (ex: Exception) {}
        BaseScreen.setSkin()  // calls our resetFont() - needed - the Skin seems to cache glyphs
    }

    /** Reduce the font list returned by platform-specific code to font families (plain variant if possible) */
    fun getAvailableFontFamilyNames(): Sequence<FontFamilyData> {
        val fontImplementation = QuantumWerewolfGame.Current.fontImplementation
            ?: return emptySequence()
        return fontImplementation.getAvailableFontFamilies()
            .sortedWith(compareBy(QuantumWerewolfGame.Current.settings.getCollatorFromLocale()) { it.localName })
    }

    /**
     * Turn a TextureRegion into a Pixmap.
     *
     * .dispose() must be called on the returned Pixmap when it is no longer needed, or else it will leave a memory leak behind.
     *
     * @return New Pixmap with all the size and pixel data from this TextureRegion copied into it.
     */
    // From https://stackoverflow.com/questions/29451787/libgdx-textureregion-to-pixmap
    fun extractPixmapFromTextureRegion(textureRegion: TextureRegion): Pixmap {
        val textureData = textureRegion.texture.textureData
        if (!textureData.isPrepared) {
            textureData.prepare()
        }
        val pixmap = Pixmap(
            textureRegion.regionWidth,
            textureRegion.regionHeight,
            textureData.format
        )
        val textureDataPixmap = textureData.consumePixmap()
        pixmap.drawPixmap(
            textureDataPixmap, // The other Pixmap
            0, // The target x-coordinate (top left corner)
            0, // The target y-coordinate (top left corner)
            textureRegion.regionX, // The source x-coordinate (top left corner)
            textureRegion.regionY, // The source y-coordinate (top left corner)
            textureRegion.regionWidth, // The width of the area from the other Pixmap in pixels
            textureRegion.regionHeight // The height of the area from the other Pixmap in pixels
        )
        textureDataPixmap.dispose() // Prevent memory leak.
        return pixmap
    }
}

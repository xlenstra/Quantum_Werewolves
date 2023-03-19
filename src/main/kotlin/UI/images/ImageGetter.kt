package UI.images

import UI.Log.debug
import UI.saving.json
import UI.util.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object ImageGetter {
    private const val whiteDotLocation = "OtherIcons/whiteDot"

    // We use texture atlases to minimize texture swapping - see https://yairm210.medium.com/the-libgdx-performance-guide-1d068a84e181
    lateinit var atlas: TextureAtlas
    private val atlases = HashMap<String, TextureAtlas>()

    // We then shove all the drawables into a hashmap, because the atlas specifically tells us
    //   that the search on it is inefficient
    private val textureRegionDrawables = HashMap<String, TextureRegionDrawable>()
    private val ninePatchDrawables = HashMap<String, NinePatchDrawable>()

    fun resetAtlases() {
        atlases.values.forEach { it.dispose() }
        atlases.clear()
        atlas = TextureAtlas("Images/game.atlas")
        atlases["game"] = atlas
    }

    /** Required every time the ruleset changes, in order to load mod-specific images */
    fun loadAtlasses() {
        textureRegionDrawables.clear()
        // These are the drawables from the base game
        for (region in atlas.regions) {
            val drawable = TextureRegionDrawable(region)
            textureRegionDrawables[region.name] = drawable
        }

        // Load base (except game.atlas which is already loaded)
        loadModAtlases()
    }

    /** Loads all atlas/texture files from a folder, as controlled by an Atlases.json */
    private fun loadModAtlases() {
        val folder = Gdx.files.internal("Images")
        // See #4993 - you can't .list() on a jar file, so the ImagePacker leaves us the list of actual atlases.
        val controlFile = folder.child("Atlases.json")
        val fileNames = (if (controlFile.exists()) json().fromJson(Array<String>::class.java, controlFile)
        else emptyArray()).toMutableList()
        for (fileName in fileNames) {
            val file = folder.child("$fileName.atlas")
            if (!file.exists()) continue
            var tempAtlas = atlases[fileName]  // fetch if cached
            if (tempAtlas == null) {
                debug("Loading %s = %s", fileName, file.path())
                tempAtlas = TextureAtlas(file)  // load if not
                atlases[fileName] = tempAtlas  // cache the freshly loaded
            }
            for (region in tempAtlas.regions) {
                if (region.name.startsWith("Skins")) {
                    val ninePatch = tempAtlas.createPatch(region.name)
                    ninePatchDrawables[region.name] = NinePatchDrawable(ninePatch)
                } else {
                    val drawable = TextureRegionDrawable(region)
                    textureRegionDrawables[region.name] = drawable
                }
            }
        }
    }


    /**
     * Colors a multilayer image and returns it as a list of layers (Image).
     *
     * @param   baseFileName    The filename of the base image.
     *                              For example: TileSets/FantasyHex/Units/Warrior
     *
     * @param   colors          The list of colors, one per layer. No coloring is applied to layers
     *                              whose color is null.
     *
     * @return  The list of layers colored. The layers are sorted by NUMBER (see example below) order
     *              and colors are applied, one per layer, in the same order. If a color is null, no
     *              coloring is performed on such layer (it stays as it is). If there are less colors
     *              than layers, the last layers are not colored. Defaults to an empty list if there
     *              is no layer corresponding to baseFileName.
     *
     * Example:
     *      getLayeredImageColored("TileSets/FantasyHex/Units/Warrior", null, Color.GOLD, Color.RED)
     *
     *      All images in the atlas that match the pattern "TileSets/FantasyHex/Units/Warrior" or
     *      "TileSets/FantasyHex/Units/Warrior-NUMBER" are retrieved. NUMBER must start from 1 and
     *      be incremented by 1 per layer. If the n-th NUMBER is missing, the (n-1)-th layer is the
     *      last one retrieved:
     *      Given the layer names:
     *          - TileSets/FantasyHex/Units/Warrior
     *          - TileSets/FantasyHex/Units/Warrior-1
     *          - TileSets/FantasyHex/Units/Warrior-2
     *          - TileSets/FantasyHex/Units/Warrior-4
     *      Only the base layer and layers 1 and 2 are retrieved.
     *      The method returns then a list in which first layer has unmodified colors, the second is
     *      colored in GOLD and the third in RED.
     */
    fun getLayeredImageColored(baseFileName: String, vararg colors: Color?): ArrayList<Image> {
        if (!imageExists(baseFileName))
            return arrayListOf()

        val layerNames = mutableListOf(baseFileName)
        val layerList = arrayListOf<Image>()

        var number = 1
        while (imageExists("$baseFileName-$number")) {
            layerNames.add("$baseFileName-$number")
            ++number
        }

        for (i in layerNames.indices) {
            val image = getImage(layerNames[i])
            if (i < colors.size && colors[i] != null)
                image.color = colors[i]
            layerList.add(image)
        }

        return layerList
    }

    fun getWhiteDot() = getImage(whiteDotLocation).apply { setSize(1f) }
    fun getDot(dotColor: Color) = getWhiteDot().apply { color = dotColor }

    fun getExternalImage(fileName: String): Image {
        // Since these are not packed in an atlas, they have no scaling filter metadata and
        // default to Nearest filter, anisotropic level 1. Use Linear instead, helps
        // loading screen and Tutorial.WorldScreen quite a bit. More anisotropy barely helps.
        val texture = Texture("ExtraImages/$fileName")
        texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
        return ImageWithCustomSize(TextureRegion(texture))
    }

    fun getImage(fileName: String?): Image {
        return ImageWithCustomSize(getDrawable(fileName))
    }

    fun getDrawable(fileName: String?): TextureRegionDrawable {
        return textureRegionDrawables[fileName] ?: textureRegionDrawables[whiteDotLocation]!!
    }

    fun getNinePatch(fileName: String?, tintColor: Color? = null): NinePatchDrawable {
        val drawable = ninePatchDrawables[fileName] ?: NinePatchDrawable(NinePatch(textureRegionDrawables[whiteDotLocation]!!.region))

        if (fileName == null || ninePatchDrawables[fileName] == null) {
            drawable.minHeight = 0f
            drawable.minWidth = 0f
        }
        if (tintColor == null)
            return drawable
        return drawable.tint(tintColor)
    }

    fun imageExists(fileName: String) = textureRegionDrawables.containsKey(fileName)
    fun ninePatchImageExists(fileName: String) = ninePatchDrawables.containsKey(fileName)


    @Deprecated("Use skin defined base color instead", ReplaceWith("BaseScreen.skinStrings.skinConfig.baseColor", "com.unciv.ui.utils.BaseScreen"))
    fun getBlue() = Color(0x004085bf)

    fun getCircle() = getImage("OtherIcons/Circle")
    fun getTriangle() = getImage("OtherIcons/Triangle")

    fun getRedCross(size: Float, alpha: Float): Actor {
        val redCross = getImage("OtherIcons/Close")
        redCross.setSize(size, size)
        redCross.color = Color.RED.cpy().apply { a = alpha }
        return redCross
    }

    fun getCrossedImage(image: Actor, iconSize: Float) = Group().apply {
        isTransform = false
        setSize(iconSize, iconSize)
        image.center(this)
        addActor(image)
        val cross = getRedCross(iconSize * 0.7f, 0.7f)
        cross.center(this)
        addActor(cross)
    }

    fun getArrowImage(align:Int = Align.right): Image {
        val image = getImage("OtherIcons/ArrowRight")
        image.setOrigin(Align.center)
        if (align == Align.left) image.rotation = 180f
        if (align == Align.bottom) image.rotation = -90f
        if (align == Align.top) image.rotation = 90f
        return image
    }


    fun getProgressBarHorizontal(
        width: Float, height: Float,
        percentComplete: Float,
        progressColor: Color,
        backgroundColor: Color): Group {
        return ProgressBar(width, height, false)
            .setBackground(backgroundColor)
            .setProgress(progressColor, percentComplete)
    }

    fun getProgressBarVertical(
        width: Float,
        height: Float,
        percentComplete: Float,
        progressColor: Color,
        backgroundColor: Color,
        progressPadding: Float = 0f): Group {
        return ProgressBar(width, height, true)
            .setBackground(backgroundColor)
            .setProgress(progressColor, percentComplete, padding = progressPadding)
    }

    class ProgressBar(width: Float, height: Float, val vertical: Boolean = true):Group() {

        var primaryPercentage: Float = 0f
        var secondaryPercentage: Float = 0f

        var label: Label? = null
        var background: Image? = null
        var secondaryProgress: Image? = null
        var primaryProgress: Image? = null

        init {
            setSize(width, height)
            isTransform = false
        }

        fun setLabel(color: Color, text: String, fontSize: Int = Constants.defaultFontSize) : ProgressBar {
            label = text.toLabel()
            label?.setAlignment(Align.center)
            label?.setFontColor(color)
            label?.setFontSize(fontSize)
            label?.toFront()
            label?.center(this)
            if (label != null)
                addActor(label)
            return this
        }

        fun setBackground(color: Color): ProgressBar {
            background = getWhiteDot()
            background?.color = color.cpy()
            background?.setSize(width, height) //clamp between 0 and 1
            background?.toBack()
            background?.center(this)
            if (background != null)
                addActor(background)
            return this
        }

        fun setSemiProgress(color: Color, percentage: Float, padding: Float = 0f): ProgressBar {
            secondaryPercentage = percentage
            secondaryProgress = getWhiteDot()
            secondaryProgress?.color = color.cpy()
            if (vertical)
                secondaryProgress?.setSize(width-padding*2, height *  max(min(percentage, 1f),0f))
            else
                secondaryProgress?.setSize(width *  max(min(percentage, 1f),0f), height-padding*2)
            if (secondaryProgress != null) {
                addActor(secondaryProgress)
                if (vertical)
                    secondaryProgress?.centerX(this)
                else
                    secondaryProgress?.centerY(this)
            }
            return this
        }

        fun setProgress(color: Color, percentage: Float, padding: Float = 0f): ProgressBar {
            primaryPercentage = percentage
            primaryProgress = getWhiteDot()
            primaryProgress?.color = color.cpy()
            if (vertical)
                primaryProgress?.setSize(width-padding*2, height *  max(min(percentage, 1f),0f))
            else
                primaryProgress?.setSize(width *  max(min(percentage, 1f),0f), height-padding*2)
            if (primaryProgress != null) {
                addActor(primaryProgress)
                if (vertical)
                    primaryProgress?.centerX(this)
                else
                    primaryProgress?.centerY(this)
            }
            return this
        }
    }

    fun getHealthBar(currentHealth: Float, maxHealth: Float, healthBarSize: Float, height: Float=5f): Table {
        val healthPercent = currentHealth / maxHealth
        val healthBar = Table()

        val healthPartOfBar = getWhiteDot()
        healthPartOfBar.color = when {
            healthPercent > 2 / 3f -> Color.GREEN
            healthPercent > 1 / 3f -> Color.ORANGE
            else -> Color.RED
        }
        healthBar.add(healthPartOfBar).size(healthBarSize * healthPercent, height)

        val emptyPartOfBar = getDot(Color.BLACK)
        healthBar.add(emptyPartOfBar).size(healthBarSize * (1 - healthPercent), height)

        healthBar.pad(1f)
        healthBar.pack()
        healthBar.background = BaseScreen.skinStrings.getUiBackground("General/HealthBar", tintColor = Color.BLACK)
        return healthBar
    }

    fun getLine(startX: Float, startY: Float, endX: Float, endY: Float, width: Float): Image {
        /** The simplest way to draw a line between 2 points seems to be:
         * A. Get a pixel dot, set its width to the required length (hypotenuse)
         * B. Set its rotational center, and set its rotation
         * C. Center it on the point where you want its center to be
         */

        // A
        val line = getWhiteDot()
        val deltaX = (startX - endX).toDouble()
        val deltaY = (startY - endY).toDouble()
        line.width = sqrt(deltaX * deltaX + deltaY * deltaY).toFloat()
        line.height = width // the width of the line, is the height of the

        // B
        line.setOrigin(Align.center)
        val radiansToDegrees = 180 / Math.PI
        line.rotation = (atan2(deltaY, deltaX) * radiansToDegrees).toFloat()

        // C
        line.x = (startX + endX) / 2 - line.width / 2
        line.y = (startY + endY) / 2 - line.height / 2

        return line
    }

    fun getSpecialistIcon(color: Color): Image {
        val specialist = getImage("StatIcons/Specialist")
        specialist.color = color
        return specialist
    }

    fun getAvailableSkins() = ninePatchDrawables.keys.asSequence().map { it.split("/")[1] }.distinct()

    fun getAvailableTilesets() = textureRegionDrawables.keys.asSequence().filter { it.startsWith("TileSets") && !it.contains("/Units/") }
        .map { it.split("/")[1] }.distinct()

    fun getAvailableUnitsets() = textureRegionDrawables.keys.asSequence().filter { it.contains("/Units/") }
        .map { it.split("/")[1] }.distinct()
}

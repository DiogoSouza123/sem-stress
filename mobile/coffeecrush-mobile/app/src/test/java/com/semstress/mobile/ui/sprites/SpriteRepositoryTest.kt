package com.semstress.mobile.ui.sprites

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * RR-21 acceptance: sprites come from the spritesheets already shipped in assets
 * (`sprites/current/items/<item>/sheet.png`, a 4x3 grid of 64x64 frames), one bitmap per item
 * instead of 12 separate frame bitmaps, with downsampling applied when the device density needs
 * a smaller frame than the 64px native asset.
 */
@RunWith(RobolectricTestRunner::class)
class SpriteRepositoryTest {

    @Test
    fun `carrega um atlas com sheet por peca e explosao`() = runTest {
        val context: Context = RuntimeEnvironment.getApplication()
        val repository = SpriteRepository(context)

        val atlas = repository.load()

        assertNotNull(atlas)
        checkNotNull(atlas)
        assertEquals(12, atlas.frameCount)
        assertEquals(4, atlas.columns)
        listOf(0, 1, 2, 3, 4, 5).forEach { value ->
            assertNotNull(atlas.pieceSheet(value))
        }
        assertNotNull(atlas.explosionSheet())
    }

    @Test
    fun `frame de indice zero comeca no canto superior esquerdo da sheet`() = runTest {
        val context: Context = RuntimeEnvironment.getApplication()
        val atlas = SpriteRepository(context).load()

        checkNotNull(atlas)
        val offset = atlas.srcOffsetFor(0)

        assertEquals(0, offset.x)
        assertEquals(0, offset.y)
    }

    @Test
    fun `segunda chamada reaproveita o atlas ja carregado`() = runTest {
        val context: Context = RuntimeEnvironment.getApplication()
        val repository = SpriteRepository(context)

        val first = repository.load()
        val second = repository.load()

        assertSame(first, second)
    }

    @Config(qualifiers = "xxhdpi")
    @Test
    fun `mantem o frame nativo quando a densidade do dispositivo pede mais detalhe`() = runTest {
        val context: Context = RuntimeEnvironment.getApplication()
        val atlas = SpriteRepository(context).load()

        checkNotNull(atlas)
        // REFERENCE_CELL_DP (56dp) at xxhdpi (density 3) needs more detail than the native 64px
        // frame, so no downsampling should occur and the native frame size is kept.
        assertEquals(64, atlas.frameSize.width)
        assertEquals(64, atlas.frameSize.height)
    }

    @Config(qualifiers = "80dpi")
    @Test
    fun `downsampling reduz o frame quando a densidade do dispositivo pede menos detalhe`() = runTest {
        val context: Context = RuntimeEnvironment.getApplication()
        val atlas = SpriteRepository(context).load()

        checkNotNull(atlas)
        // REFERENCE_CELL_DP (56dp) at density 0.5 (80dpi) targets ~28px, well under the 32px
        // half-step, so the sheet should be decoded at half resolution (32px frames).
        assertEquals(32, atlas.frameSize.width)
        assertEquals(32, atlas.frameSize.height)
    }
}

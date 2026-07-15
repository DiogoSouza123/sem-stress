package com.semstress.mobile.ui.sprites

import android.content.Context
import com.semstress.mobile.engine.EmptyCupState
import com.semstress.mobile.engine.Match3Engine
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
 * RR-21 acceptance, updated for the v2 HD pack: regular and special pieces are single static
 * 256px frames (`sprites/current/items/<item>/sheet.png`), the explosion keeps the animated 4x3
 * grid of 64px frames, and downsampling still applies when the device density needs less detail
 * than the native piece frame.
 */
@RunWith(RobolectricTestRunner::class)
class SpriteRepositoryTest {

    @Test
    fun `carrega um atlas com sheet por peca, especiais e explosao`() = runTest {
        val context: Context = RuntimeEnvironment.getApplication()
        val repository = SpriteRepository(context)

        val atlas = repository.load()

        assertNotNull(atlas)
        checkNotNull(atlas)
        listOf(0, 1, 2, 3, 4, 5).forEach { value ->
            val sheet = atlas.pieceSheet(value)
            assertNotNull(sheet)
            assertEquals(1, checkNotNull(sheet).frameCount)
        }
        assertNotNull(atlas.pieceSheet(Match3Engine.SPECIAL_GRINDER))
        assertNotNull(atlas.pieceSheet(Match3Engine.SPECIAL_FRENCH_PRESS))
        val explosion = atlas.explosionSheet()
        assertNotNull(explosion)
        assertEquals(12, checkNotNull(explosion).frameCount)
        assertEquals(4, explosion.columns)
    }

    @Test
    fun `todos os estados codificados da xicara vazia resolvem para o mesmo sprite`() = runTest {
        val context: Context = RuntimeEnvironment.getApplication()
        val atlas = SpriteRepository(context).load()

        checkNotNull(atlas)
        val fresh = atlas.pieceSheet(EmptyCupState.encode(EmptyCupState.INITIAL_TURNS, 0))
        val worn = atlas.pieceSheet(EmptyCupState.encode(1, 7))

        assertNotNull(fresh)
        assertSame(fresh, worn)
    }

    @Test
    fun `frame de indice zero comeca no canto superior esquerdo e o seguinte avanca uma coluna`() = runTest {
        val context: Context = RuntimeEnvironment.getApplication()
        val atlas = SpriteRepository(context).load()

        checkNotNull(atlas)
        val explosion = checkNotNull(atlas.explosionSheet())

        assertEquals(0, explosion.srcOffsetFor(0).x)
        assertEquals(0, explosion.srcOffsetFor(0).y)
        assertEquals(explosion.frameSize.width, explosion.srcOffsetFor(1).x)
        // Pieces are single-frame: any frame index resolves to the same (only) frame.
        val piece = checkNotNull(atlas.pieceSheet(0))
        assertEquals(0, piece.srcOffsetFor(7).x)
        assertEquals(0, piece.srcOffsetFor(7).y)
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
        // REFERENCE_CELL_DP (56dp) at xxhdpi (density 3) targets ~168px; half of the native 256px
        // frame (128px) would be too small, so no downsampling should occur.
        val piece = checkNotNull(atlas.pieceSheet(0))
        assertEquals(256, piece.frameSize.width)
        assertEquals(256, piece.frameSize.height)
    }

    @Config(qualifiers = "80dpi")
    @Test
    fun `downsampling reduz o frame quando a densidade do dispositivo pede menos detalhe`() = runTest {
        val context: Context = RuntimeEnvironment.getApplication()
        val atlas = SpriteRepository(context).load()

        checkNotNull(atlas)
        // REFERENCE_CELL_DP (56dp) at density 0.5 (80dpi) targets ~28px; the 256px native frame
        // can be decoded at 1/8 resolution (32px) while still covering the target.
        val piece = checkNotNull(atlas.pieceSheet(0))
        assertEquals(32, piece.frameSize.width)
        assertEquals(32, piece.frameSize.height)
    }
}

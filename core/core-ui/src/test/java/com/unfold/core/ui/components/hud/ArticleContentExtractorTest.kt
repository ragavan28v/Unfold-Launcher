package com.unfold.core.ui.components.hud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleContentExtractorTest {
    @Test
    fun `extracts real article paragraphs instead of headline metadata`() {
        val html = """
            <html>
              <head>
                <title>AI market jumps as investors eye next wave</title>
                <meta name="description" content="AI market jumps as investors eye next wave" />
              </head>
              <body>
                <article>
                  <h1>AI market jumps as investors eye next wave</h1>
                  <p>Analysts said the rally was driven by better-than-expected earnings in chipmakers and software vendors.</p>
                  <p>Executives warned that demand remains uneven across enterprise buyers, though many firms are still expanding AI budgets.</p>
                </article>
              </body>
            </html>
        """.trimIndent()

        val extracted = ArticleContentExtractor.extract(html, "AI market jumps as investors eye next wave")

        assertNotNull(extracted)
        assertTrue(extracted!!.body.contains("Analysts said the rally was driven"))
        assertFalse(extracted.body.contains("AI market jumps as investors eye next wave"))
    }

    @Test
    fun `rejects title-only metadata as article body`() {
        val html = """
            <html>
              <body>
                <div class="content">
                  <h1>Startup raises $20M for climate software</h1>
                </div>
              </body>
            </html>
        """.trimIndent()

        val extracted = ArticleContentExtractor.extract(html, "Startup raises $20M for climate software")

        assertTrue(extracted == null)
    }
}

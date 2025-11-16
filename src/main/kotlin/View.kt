package com.drbrosdev

import kotlinx.html.*

fun HTML.createUrlPage(
    basePath: String,
) {
    headContent(
        basePath = basePath,
        pageTitle = "Y URL"
    )

    body(
        classes = "h-screen w-screen font-mono"
    ) {
        mainLayout {
            p(classes = "text-2xl text-center") { +"Yet Another URL Shortener" }
            postForm(
                encType = FormEncType.textPlain,
                action = "/create-url",
                classes = "flex flex-col items-center w-full space-y-12"
            ) {
                input(
                    type = InputType.url,
                    classes = "text-4xl text-center w-full focus:outline-hidden w-full"
                ) {
                    required = true
                    name = "url"
                    id = "url"
                    placeholder = "Your URL here."
                }

                hopButton { +"Go" }
            }
        }
    }
}

fun HTML.shortUrlCreatedPage(
    basePath: String,
    createdUrl: String,
) {
    headContent(pageTitle = "Success!", basePath = basePath)

    body(classes = "w-screen h-screen font-mono") {
        mainLayout {
            h1(classes = "text-3xl") { +"Your short URL is" }
            a(
                href = createdUrl,
                target = null,
                classes = "text-3xl font-bold hover:underline visited:text-purple-600"
            ) {
                +createdUrl
            }
        }
    }
}

// =====

private fun HTML.hopFooter() {
    // TODO
}

private inline fun FlowContent.mainLayout(
    crossinline block: FlowContent.() -> Unit
) {
    div(classes = "relative w-screen min-h-screen bg-black") {
        div(classes = "flex w-screen h-screen items-center justify-center") {
            div(classes = "text-white p-10 flex flex-col items-center space-y-12 w-full") {
                block()
            }
        }
    }
}

private fun HTML.headContent(
    basePath: String,
    pageTitle: String = "",
    block: HEAD.() -> Unit = {}
) {
    head {
        title { +pageTitle }
        meta(name = "description", content = "Yet another URL shortener.")
        link(rel = "icon", href = "$basePath/public/favicon.ico", type = "image/x-icon")
        link(rel = "canonical", href = basePath)
        script(src = "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4") { }

        // open-graph
        meta(property = "og:site_name", content = "Y URL")
        meta(property = "og:url", content = basePath)
        meta(property = "og:image", content = "$basePath/public/logo.png")
        meta(property = "og:description", content = "Yet another URL shortener.")
        meta(property = "og:title", content = "Y URL")
        // twitter
        meta(name = "twitter:title", content = "Y URL")
        meta(name = "twitter:description", content = "Yet another URL shortener.")
        meta(name = "twitter:image", content = "$basePath/public/logo.png")

        block()
    }
}

private fun FlowOrMetaDataOrPhrasingContent.meta(property: String, content: String) = meta {
    attributes["property"] = property
    this.content = content
}

private inline fun FlowContent.hopButton(
    crossinline block: FlowContent.() -> Unit
) {
    button(
        classes = """
            text-white
            bg-transparent
            box-border
            border 
            border-bg-white
            transition
            hover:bg-white
            hover:text-black
            focus:ring-2 focus:ring-white-300 
            shadow-xs font-medium leading-5 rounded-base text-sm px-4 py-2.5 focus:outline-none cursor-pointer w-xs
        """.trimIndent(),
        block = block
    )
}

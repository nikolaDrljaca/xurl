package com.drbrosdev

import kotlinx.html.*

fun HTML.createUrlPage(
    basePath: String,
) {
    headContent(pageTitle = "Y URL", basePath = basePath)

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

            a(
                href = basePath,
                target = null,
                classes = "$hopButtonClasses text-center"
            ) {
                +"Go again"
            }
        }
    }
}

// =====

private fun FlowContent.hopFooter() =
    footer(classes = "flex text-onbrand p-5") {
        a(
            classes = "text-sm underline visited:text-purple-700",
            href = "https://github.com/nikolaDrljaca/yurl"
        ) {
            +"View on GitHub"
        }
    }

private inline fun FlowContent.mainLayout(
    crossinline block: FlowContent.() -> Unit
) {
    div(classes = "relative w-screen h-screen bg-brand") {
        div(classes = "flex flex-col w-screen h-screen items-center justify-between") {
            div(classes = "text-onbrand p-1 lg:p-10 flex flex-col items-center justify-center space-y-12 w-full h-full") {
                block()
            }

            hopFooter()
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
        link(rel = "icon", href = "/favicon.ico", type = "image/x-icon")
        link(rel = "canonical", href = basePath)
        script(src = "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4") { }

        style(type = "text/tailwindcss") {
            unsafe {
                raw(
                    """
              @theme {
                --color-brand: #222831;
                --color-onbrand: #DFD0B8;
              }
                """
                )
            }
        }

        // open-graph
        meta(property = "og:site_name", content = "Y URL")
        meta(property = "og:url", content = basePath)
        meta(property = "og:image", content = "$basePath/logo.png")
        meta(property = "og:description", content = "Yet another URL shortener.")
        meta(property = "og:title", content = "Y URL")
        // twitter
        meta(name = "twitter:title", content = "Y URL")
        meta(name = "twitter:description", content = "Yet another URL shortener.")
        meta(name = "twitter:image", content = "$basePath/logo.png")

        block()
    }
}

private fun FlowOrMetaDataOrPhrasingContent.meta(property: String, content: String) = meta {
    attributes["property"] = property
    this.content = content
}

private val hopButtonClasses = """
    text-onbrand
    bg-transparent
    box-border
    border 
    border-bg-onbrand
    transition
    hover:bg-onbrand
    hover:text-brand
    focus:ring-2 focus:ring-white-300 
    shadow-xs font-medium leading-5 rounded-base text-sm px-4 py-2.5 focus:outline-none cursor-pointer w-xs
""".trimIndent()

private inline fun FlowContent.hopButton(
    crossinline block: FlowContent.() -> Unit
) {
    button(
        classes = hopButtonClasses,
        block = block
    )
}

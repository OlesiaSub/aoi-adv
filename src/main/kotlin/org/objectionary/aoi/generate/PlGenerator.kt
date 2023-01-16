package org.objectionary.aoi.generate

import org.objectionary.aoi.generate.util.*
import org.objectionary.aoi.sources.SourcesExtractor
import org.objectionary.deog.abstract
import org.objectionary.deog.base
import org.objectionary.deog.name
import org.objectionary.deog.packageName
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File

typealias GraphAbstracts = MutableMap<String, MutableSet<Node>>
const val prologFile = "proloog.pl"

class PlGenerator {
    fun generatePrologScripts(path: String) {
        File(prologFile).createNewFile()
        val sourcesExtractor = SourcesExtractor()
        val documents = sourcesExtractor.collectDocuments(path)
        documents.forEach {
            generatePrologScript(it.key)
        }
        rules()
    }

    private fun generatePrologScript(document: Document) {
        val objects: MutableList<Node> = mutableListOf()
        val docObjects = document.getElementsByTagName("o")
        for (i in 0 until docObjects.length) {
            objects.add(docObjects.item(i))
        }
        val obj = document.getElementsByTagName("objects").item(0)
        val children = obj.childNodes ?: return
        for (i in 0 until children.length) {
            val node = children.item(i)
            abstract(node)?.let {
                collectNodeInfo(node)
            }
        }
    }

    private fun collectNodeInfo(node: Node) {
        val children = node.childNodes ?: return
        var offset = 0
        for (i in 0 until children.length) {
            if (i + offset >= children.length) break
            val child = children.item(i + offset)
            abstract(child)?.let {
                containsAttrFact(name(node)!!, name(child)!!)
            }
            base(child)?.let {
                if (!it.startsWith(".")) {
                    val (txt, j, name) = walkDotChain(child)
                    val l = txt.split('.')
                    if (l.size >= 3) {
                        appliedFact(l[0], l[1], l[2])
                    } else if (l.size == 2) {
                        name(node)?.let {n ->
                            appliedFact(n, l[0], l[1])
                        }
                    }
                    name?.let {n ->
                        if (n == "@") {
                            parentFact(txt, getFqn(name(node)!!))
                        } else {
                            isInstanceFact(n, txt)
                        }
                    }
                    offset += j
                }
            }
            collectNodeInfo(child)
        }
    }

    /**
     * @return concatenated test of bases, nu,ber of passed children, true if name == "@" else false
     */
    private fun walkDotChain(
        node: Node
    ): Triple<String, Int, String?> {
        var txt = base(node)
        var i = 0
        var sibling = node.nextSibling?.nextSibling
        while (base(sibling)?.startsWith(".") == true) {
            txt += base(sibling)
            i++
            var tmp = sibling?.nextSibling
            tmp?.attributes ?: run { tmp = tmp?.nextSibling }
            if (base(tmp)?.startsWith('.') == true) sibling = tmp
            else break
        }
        return if (i > 0) Triple(txt!!, i, name(sibling)) else Triple(txt!!, i, name(node))
    }

    private fun getFqn(name: String): String {
//        var fqn = name
//        var parent = par
//        while (name(parent) != null) {
//            fqn = "${name(parent)}.$fqn"
//            parent = parent.parentNode
//        }
//        return fqn
        return name
    }

    @Suppress("PARAMETER_NAME_IN_OUTER_LAMBDA")
    private fun abstracts(objects: MutableList<Node>) {
        val abstracts: GraphAbstracts = mutableMapOf()
        objects.forEach {
            val name = name(it)
            if (abstract(it) != null && name != null) {
                abstracts.getOrPut(name) { mutableSetOf() }.add(it)
            }
        }
    }
}
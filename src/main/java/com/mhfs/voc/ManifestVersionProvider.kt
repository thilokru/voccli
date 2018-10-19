package com.mhfs.voc

import picocli.CommandLine

class ManifestVersionProvider: CommandLine.IVersionProvider {
    override fun getVersion(): Array<String> {
        return arrayOf(this::class.java.`package`.implementationVersion)
    }
}
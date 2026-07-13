package org.liamjd.bascule

object TestProjectYaml {
    private const val CONFIG_RESOURCE = "project-config.yaml"
    private const val TEMPLATES_PLACEHOLDER = "__TEMPLATES_PATH__"

    fun load(templatesPath: String = "liamjd-theme/templates"): String {
        val yamlTemplate = requireNotNull(TestProjectYaml::class.java.classLoader.getResource(CONFIG_RESOURCE)) {
            "Missing test config resource: $CONFIG_RESOURCE"
        }.readText()

        return yamlTemplate.replace(TEMPLATES_PLACEHOLDER, templatesPath)
    }
}

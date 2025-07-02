package de.ur.apert

fun getCamundaURL(): String {
    return getBaseUrl() + ":8080"
}

fun getElasticURL(): String {
    return getBaseUrl() + ":9200"
}
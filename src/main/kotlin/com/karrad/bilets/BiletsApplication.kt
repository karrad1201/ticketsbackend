package com.karrad.bilets

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class BiletsApplication

fun main(args: Array<String>) {
    runApplication<BiletsApplication>(*args)
}

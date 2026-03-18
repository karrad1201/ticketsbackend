package com.karrad.bilets

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BiletsApplication

fun main(args: Array<String>) {
	runApplication<BiletsApplication>(*args)
}

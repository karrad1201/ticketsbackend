package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.City

interface CityRepository {
    fun findAll(): List<City>
}

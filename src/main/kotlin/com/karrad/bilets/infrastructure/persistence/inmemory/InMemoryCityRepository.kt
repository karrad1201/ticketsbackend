package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.repository.CityRepository
import java.util.UUID

class InMemoryCityRepository : CityRepository {

    private val cities: List<City> = listOf(
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000001"),
            label = "Москва",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000001"), label = "Москва")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000002"),
            label = "Санкт-Петербург",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000002"), label = "Санкт-Петербург")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000003"),
            label = "Элиста",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000003"), label = "Республика Калмыкия")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000004"),
            label = "Казань",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000004"), label = "Республика Татарстан")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000005"),
            label = "Уфа",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000005"), label = "Республика Башкортостан")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000006"),
            label = "Краснодар",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000006"), label = "Краснодарский край")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000007"),
            label = "Сочи",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000006"), label = "Краснодарский край")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000008"),
            label = "Ростов-на-Дону",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000007"), label = "Ростовская область")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000009"),
            label = "Волгоград",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000008"), label = "Волгоградская область")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000010"),
            label = "Астрахань",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000009"), label = "Астраханская область")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000011"),
            label = "Новосибирск",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000010"), label = "Новосибирская область")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000012"),
            label = "Екатеринбург",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000011"), label = "Свердловская область")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000013"),
            label = "Нижний Новгород",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000012"), label = "Нижегородская область")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000014"),
            label = "Самара",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000013"), label = "Самарская область")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000015"),
            label = "Челябинск",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000014"), label = "Челябинская область")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000016"),
            label = "Омск",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000015"), label = "Омская область")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000017"),
            label = "Красноярск",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000016"), label = "Красноярский край")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000018"),
            label = "Пермь",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000017"), label = "Пермский край")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000019"),
            label = "Воронеж",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000018"), label = "Воронежская область")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000020"),
            label = "Саратов",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000019"), label = "Саратовская область")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000021"),
            label = "Тюмень",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000020"), label = "Тюменская область")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000022"),
            label = "Владивосток",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000021"), label = "Приморский край")
        ),
        City(
            id = UUID.fromString("b0000000-0000-0000-0000-000000000023"),
            label = "Иркутск",
            subject = Subject(id = UUID.fromString("a0000000-0000-0000-0000-000000000022"), label = "Иркутская область")
        )
    )

    override fun findAll(): List<City> = cities
}

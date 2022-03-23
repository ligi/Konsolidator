plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "org.devcon"
version = "0.1"

val kethereum_version = "0.85.7"

application {
    mainClassName = "org.komputing.konsolidator.MainKt"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.github.komputing.kethereum:keystore:$kethereum_version")
    implementation("com.github.komputing.kethereum:model:$kethereum_version")
    implementation("com.github.komputing.kethereum:crypto:$kethereum_version")
    implementation("com.github.komputing.kethereum:crypto_impl_bouncycastle:$kethereum_version")
    implementation("com.github.komputing.kethereum:extensions_transactions:$kethereum_version")
    implementation("com.github.komputing.kethereum:eip1559_signer:$kethereum_version")
    implementation("com.github.komputing.kethereum:eip155:$kethereum_version")
    implementation("com.github.komputing.kethereum:erc55:$kethereum_version")
    implementation("com.github.komputing.kethereum:eip1559_feeOracle:$kethereum_version")
    implementation("com.github.komputing.kethereum:rpc:$kethereum_version")
    implementation("com.github.komputing.kethereum:rpc_min3:$kethereum_version")
    implementation("com.github.komputing.kethereum:erc20:$kethereum_version")
    implementation("com.github.walleth:khex:0.6")
    implementation("com.github.komputing:khex:1.0.0-RC6")
    implementation("com.michael-bull.kotlin-retry:kotlin-retry:1.0.9")


    implementation("com.natpryce:konfig:1.6.10.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
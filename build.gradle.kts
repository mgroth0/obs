configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
  sourceSets {
	val commonMain by getting {
	  dependencies {
		implementations(
		  ":k:klib".auto(), // this is just an example, feel free to remove
		  handler = this
		)
	  }
	}
  }
}
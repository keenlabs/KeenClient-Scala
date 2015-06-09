package test

import org.specs2.mutable.Specification

import io.keen.client.scala.util.ScopedKeys

class ScopedKeySpec extends Specification {

  val apiKey = "80ce00d60d6443118017340c42d1cfaf"

  "Scoped Keys should" should {

    "handle encryption and decryption" in {

      val options = """{
    "filters": [{
        "property_name": "account_id",
        "operator": "eq",
        "property_value": 123
    }],
    "allowed_operations": [ "read" ]
}"""

      val enciphered = ScopedKeys.encrypt(apiKey, options)
      options must beEqualTo(ScopedKeys.decrypt(apiKey, enciphered))
    }
  }
}

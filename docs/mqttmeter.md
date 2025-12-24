# mqtt enabled energy meter

Builds off the SixNode Advanced energy meter

The meter GUI should accept the following parameters:

* Meter Name: The friendly name of the meter.
* MQTT Name: The name of the MQTT server to connect to. This friendly name will turn green if it matches a server configuration stanza in the server configuration file.
* A read-only field should contain the meter ID, a 6 digit number that is randomly generated on placement and stored in both the meter's NBT and Eln's server NBT to prevent conflicts. (If all 6 digit combinations are exhaused, increase digit count)
* Button that is Green when the circuit is on and Red when it is off. This buton allows the meter to be turned off or on by any player.

Other fields on the meter are superflous and not required for this meter. The time and kJ dials on the meter should provide the same information we provide over MQTT.

Copy the textures so that I can make specific edits to them in GIMP. The same model files can be used if possible.

## Server Configuration

In a new section of the mod configuration called "MQTT" we add the following:

The server configuration file should contain mqtt server name (from above)/username/password/mqtt server uri/Eln MQTT prefix information for each MQTT server that the meter should be able to connect to. A list of these should be able to be specified.

For example, if the config used json syntax:

```json
{
    "mqtt": [
        {
            "name": "server1",
            "username": "user1",
            "password": "",
            "uri": "tcp://192.168.1.1:1883",
            "prefix": ""
        },
        {
            "name": "server2"
        }
    ]
}
```

If any parameters are missing (such as is the case for "server2" above), the mqtt endpoint will not be enabled and an error thrown in the log. The prefix may be blank.

The server configuration should also contain a MQTT disable option for security.

## Provides the following MQTT topics:

$meter is the meter ID (see above)

### Read Status:

* `eln/meter/$meter/stat/power` - the current power usage in Watts
* `eln/meter/$meter/stat/energy` - the energy usage in Wh since the last reset
* `eln/meter/$meter/stat/time` - the time since the last reset in seconds
* `eln/meter/$meter/stat/current` - the current current in Amperes
* `eln/meter/$meter/stat/voltage` - the current voltage in Volts
* `eln/meter/$meter/stat/status` - the meter status, either `on` or `off`

### Read Info:

* `eln/meter/$meter/info/name` - the short name of the meter
* `eln/meter/$meter/info/level` - the dimension of the meter
* `eln/meter/$meter/info/pos` - the position of the meter in the world

### Write Control:

* `eln/meter/$meter/ctrl/reset` - resets the meter energy reading to zero
* `eln/meter/$meter/ctrl/status` - sets the meter status, either `on` or `off`


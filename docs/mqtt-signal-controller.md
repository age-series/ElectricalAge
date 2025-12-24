# MQTT Signal Controller

Uses the model of the Signal Processor and has a new texture. This device shares many of the same ideas as the MQTT enabled energy meter.

The GUI should accept the following parameters:

* Signal Controller Name: The friendly name of the meter.
* MQTT Name: The name of the MQTT server to connect to. This friendly name will turn green if it matches a server configuration stanza in the server configuration file.
* A read-only field should contain the signal controller ID, a 6 digit number that is randomly generated on placement and stored in both the meter's NBT and Eln's server NBT to prevent conflicts. (If all 6 digit combinations are exhaused, increase digit count) Note: These are distinct from the meter ID's used in the power meters.
* A tri-state button that enables or disables each of the 4 signal input/output "ports", with options "D" (tip text Disabled), "R" (tip text Read), "W" (tip text Write). The 3 inputs on the signal processor are A, B, C like usual and are bidirectional ports for the signal controller. The fourth bidirectional port is the "output" on the Signal processor, call it "D".

If the signal controller input/output port is set to disabled, it will not report to MQTT.

## MQTT Topics:

### Read Status:

* `eln/signal/$id/stat/port/$port/volts` - the current voltage on the signal controller port in volts
* `eln/signal/$id/stat/port/$port/normal` - the normalized value of the voltage (0v = 0%, 5v = 100%)

### Read Info:

* `eln/signal/$id/info/name` - the short name of the meter (retained)
* `eln/signal/$id/info/level` - the dimension of the meter (retained)
* `eln/signal/$id/info/pos` - the position of the meter in the world (retained)
* `eln/signal/$id/info/port/$port` - the status of the port on the signal controller `read` or `write` (retained) - if the port is disabled, this topic will not be present for that port

### Write Control:

* `eln/signal/$id/ctrl/port/$port/normal` - sets the normalized value of the voltage (0v = 0%, 5v = 100%)
* `eln/signal/$id/ctrl/port/$port/volts` - sets the voltage on the signal controller port in volts
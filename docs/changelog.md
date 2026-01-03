## 1.16.14

- Fixes the steam generation taking place when the temperature is not high enough for steam
- Fixes power being misinterpreted by cables

## 1.16.13

- Fixes negative thermal power not causing negative temperatures
- Adds tooltip to thermal cables, showing minimum temperature

## 1.16.12

- Heat exchanger crafting
- Creative current sources
- other bugs fixed

## 1.16.11

### Adds:

- Alternative recipe to make iron cables when your mods conflict

### Changes:

- Less explody poles by reducing current inrushes (protip: aparently buggy)

## 1.16.10

### Adds:

- Thermal Heat Exchanger

### Changes:

- DC/DC now uses copper cables, and reverse bug is fixed
- Performance imporvements in the MNA
- Slight thermal system changes

The thermal heat exchanger can be currently used for two things:

- Convert back and forth between ic2hotcoolant and ic2coolant and Eln heat
- Convert water into (ic2)steam (10x steam per water)

The thermal heat exchanger requires a signal input on the far side from the heat input. The fluids must flow from bottom to top of the exchanger.

The exchanger must be placed on the ground before the bottom pipes are connected. The exchanger will not move fluids on it's own, so you will need to pull the fluids out of the internal tank at the top.

## 1.16.9

### Adds:

- Rotary motor has an item texture now!

### Changes:

- vDC/DC has new textures in GUI and on device
- DC/DC code rewritten in Kotlin, expect less bugs
- en_US language file has updated
- DC/DC no longer accepts rubber based cables, and doesn't have voltage limits anymore!

## 1.16.8

### Fixes:

- Lighting bug with lamp supplies and such

## 1.16.7

### Fixes:

- T1 power poles can now handle 51.2kV like the T2 poles (since why not)

### Added:

- Rotary motor - takes 24mB/t of fuel

It is best to let the rotary motor start up to full speed (you can use a shaft motor to speed this up) and then use a clutch to slowly introduce power into flywheels or an entire shaft network. It can provide a little over 50kW of power sustained, but it's not really designed for base power. Use steam turbines for best fuel efficiency.

Efficiency curve of the Rotary Motor:

![](https://user-images.githubusercontent.com/14130505/93027986-bc5d5700-f5de-11ea-8546-8da08c380b06.png)

Placement of the rotary motor is a little wonky due to the shaft location. Place a single block, and then place it on top of that block. Then, remove that block and profit. Signal input accepted on the front edge of the shaft motor, next to where the shaft is.

1[](https://user-images.githubusercontent.com/14130505/93028053-41487080-f5df-11ea-9fe8-beefbcd13141.png)

## 1.16.6

### Fixes:

- Lamps now properly connect to wires and lamp supplies
- Lamps no longer consume infinite current
- Lamps no longer cause certain lighting bugs
- Bulb life actually works now and ranges from 4 to 512 real-life hours
- `mod()` operator in the sigproc: takes two args and runs mod on them

## 1.16.5

### Fixes:

- The variable DC/DC converters no longer corrupt the world AND it now attempts to try and fix corrupted worlds. Neato!
- Null item stacks in devices that accepts items no longer crash the client and server (as accidentally introduced last version)

## 1.16.4

### Added:

- Christmas Trees
- Holiday Candles
- String lights
- Grinch mode
- Non-swinging variants of suspended lamps

### Fixes:

- Tooltip fixes on machines

### Bugs:

- The vDC/DC's *cannot* be placed next to one another or it will corrupt your world. If this happens, contact a developer to get your world fixed.

## 1.15.11

### Changed:

- vDC/DC converter now allows basic uninsulated copper cables to be used in the coils
- Haxorian balance patches...
    - Batteries last 1/4 of the time they used to
    - Machines can draw more power (and do things faster!)
    - Recycling old batteries is now less

## 1.15.8

### Adds:

- Variable DC/DC Converter - allows for much more flexible control over the voltage output.

You can boost or buck based on the orientation of the vDC/DC on the ground. One can use this block to create a stable output voltage on one side with some control circuitry.

The ratio range is from 1.0 to 0.1, the input signal is clamped to prevent too high of a voltage output.

Crafting the device is the same as a DC/DC except that there's a cheap chip between the copper cables.

## 1.15.7

Clutches don't allow NaN as an input anymore.

## 1.15.6

It says that we fixed NaN's around shafts, but that's a lie.

## 1.15.5

It apparently didn't fix anything.

## 1.15.4

### Changes:

- Mining pipes and alloy dust are a bit less grindy
- Updated version checker to include stat feature

### Bugfixes:

- Added matrix debugging information to WAILA
- Fixed autominer to use full chest
- Fixed creative flywheel throwing bug
- Cached important shaft variables

## 1.15.3 (LLv11.1)

### Added:

- Ability to cancel grid connections!

### Changed:

- Rebalancing of clutches and clutch plates

### Bugfixes:

- Datalogger rotations

## 1.15.2 (LLv11)

### Added:

- HV and VHV furnace heating cores for the electric furnace
- New Shaft objects!
    - Fixed shaft: does not rotate, can be used for additional braking power
    - Clutch: engage and disengages shafts at will!
- MNA state exporter for debugging
- Power sockets now work
- New build tools
- Updated version checker
- Much more build information
- `/eln version` command for build information ingame!
- You can now craft Nixie Tubes and Digital Displays!
- Haxorian Changes:
    - Arc furnace, arc clay, arc metal, creative cables, etc.
- Config copy tool: copies configs and items in and out of devices. Use shift right click to copy and right click to paste. Very useful!

### Changed:

- Various motor balance changes
- Shaft Hubs now do vertical!
- Turrets ignore invulnerability timer
- Updated developer list
- Modifications to Travis CI build system
- Updated contribution guide
- You can color displays and outlets
- Many crafting recipies modified

### Bugfixes:

- Msc fixes from upstream for build tools
- Single use batteries are now actually single use
- Texture fixes with thermal probe chip
- Wire ID fix from bad Haxorian changes

## -1.14.6 (LLv10)

### Added:

- microfarad support (and micro support for most other units)
- Portable capacitors and capacitor packs
- Nixie Tubes!
- Digital Displays!
- Dataloggers now support the "No Unit" `Unit` type, which shows just the numbers.

### Changed:

- Updatesd were made to the language files
- German language file
- Motor behavior is modified, less noise from shaft motors!
- The "Haxorian Balance Patch", which modifies the balance of armor, tools, and portable batteries.
- Electrical meter information about batteries on the ground is more useful

### Bugfixes:

- Fixed render glitches with biome colored blocks (grass, leaves) on SixNode
- Fixed Quartz not being able to be placed on SixNode as a cover (as well as other blocks)
- Rendering bugs with the printouts of displays are now fixed.

## -1.14.5 (LLv9)

### Added:

- Shaft motor: Create shaft power efficiently!
- Signal Bus Cable: Combine up to 16 signals in a single signal cable! Breakout of colors required, colored Signal Bus cables are NOT supported.

### Changed:

- Updates were made to the language file. Spelling mistakes, clarifications, and other fixes included

### Bugfixes:

- Brushes (for coloring wires) are fixed in Multiplayer servers
- Sound bug has been resolved, and a configruation option added. A value of 16 is generally good enough but some installations may need it as low as 6.
- Fixes to initial rendering bugs of Signal Bus cables in LLv8

### Removed:

- We removed the broken language file helper

## 1.14.2

### Bugfixes

- Fixed a world-destroying bug in the saving code, which only affected Windows.

  If you've been affected, see
  https://github.com/Electrical-Age/ElectricalAge/issues/673 for a possible
  recovery method.
  
- Typing /eln without a parameter will now list the commands.

## 1.14.1

### Features

- IC2 steam should now work in the steam turbine. You won't get distilled water back, mind you.

### Bugfixes

- Some debug-prints were not marked as debug, and could spam players under certain circumstances.

- The Sample-and-Hold chip had a single global sample across all chips.

## 1.14.0

![Fuel Heat Furnace](https://i.imgur.com/BaaoHiY.png)

This time you get a lot of small features, plus bugfixes.

Did you know? Heat turbines get dramatically less efficient if you don't run
them at prcisely the right temperature and voltage. This means that, although
they aren't bad when optimally used, actually doing so requires clever signal
processing. You should attempt to use the minimum number of heat turbines
possible at any given time.

But I know that most of us will keep building banks like this one.

You can find the download
[at Curseforge](https://minecraft.curseforge.com/projects/electrical-age), as
usual.

### Community spotlight

Nesze is [building a digital computer](https://puu.sh/tL65Y/ed5af7c4ee.png)
using signal processors...

We're looking forward to seeing the result.

---

There is an ongoing series of tutorials, to be found
[on the wiki](https://wiki.electrical-age.net/index.php?title=Examples), and
which now includes an embryonic power-pole tutorial.

As always, we will grant wiki editing access to anyone who shows up
[in gitter](https://gitter.im/Electrical-Age/Support) or
[on irc](https://qchat.rizon.net/?channels=electricalage) and asks.

### Features in this release

- WAILA support has been expanded to (nearly) all blocks. Enjoy!

- The transformer has been renamed and re-modelled. It's now called the DC-DC
  converter. This should reduce confusion.
  
- Added a NOT function to the signal processor.
  Use it as such, for example: "!(B*C)"

- Added a Fuel Heat Furnace.

  This works like the normal heat furnace, but can produce up to 25kW of
  heat. It burns heavy oils and gasolines, but not gases. If you're using a mod
  such as Immersive Engineering to fractionate your oil, then this is a great
  option for making the diesel oil useful.
  
  For gasoline, unless you're extremely careful, this will not be nearly as
  efficient as burning it in the gas turbine. Even if you *are* extremely
  careful, it still won't be quite as good. It is, however, excellent for
  running a bank of starter turbines.
  
- Completely rewrote the way we loop sounds.

  You'll notice this the moment you step into a turbine hall. Sounds are now
  looped client-side, which should prevent glitches from server lag, and
  pitch/volume are adjusted appropriately.
  
  There's a high chance of bugs in this code, though we think we've squashed the
  worst ones. If you notice anything broken, please tell us.
  
  We've also replaced some of the sound files.
  
- Added sound to the gas turbine, steam turbine and generators.

  Whooosh!
  
- Rebalanced the heating value of the various liquid fuels.

  In general they're all slightly increased, but some are far more so than
  others. You'll find that one bucket of gasoline lasts far longer than one
  bucket of ethanol, and additionally the power output of the gas turbine is
  capped by maximum *fluid throughput*, not kilowatt output. This means you'll
  need more gas turbines if you're burning a lesser fuel.
  
  The values used match real life, but the game-balance intent is to encourage
  using non-renewable fuel. Install Buildcraft! Make use of those oil wells! Two
  thousand buckets will last a while, almost certainly.
  
- Rebalanced the gas turbine efficiency curve.

  To make up for the above, the gas turbine must now be spun up to 650 rads/s
  before it will start working. The efficiency curve is tighter in general;
  you'll see about 85% efficiency at 900 rads/s, hopefully encouraging more
  complex builds.
  
  Mind you, 85% is still likely better than what you got before the fuel value
  rebalancing.
  
- You are now able to burn a few more fuel variants...

  Anything not already on the list is due to oversights. Take a look at
  [FuelRegistry.kt](https://github.com/Electrical-Age/ElectricalAge/blob/3e7db53eac084b4f2770139949630d01f72a8767/src/main/java/mods/eln/fluid/FuelRegistry.kt),
  check if your preferred fuel is there, and if not consider opening an issue or
  filing a pull request.
  
- Many items can now be auto-inserted into blocks by right-clicking them.

  Try it!

- New config option for oredicting chips. This is mostly useful for GregTech players.

- Added 2x3 solar panels.

  These panels have a footprint of 2x3 blocks, but a maximum power output of 8
  smaller panels, and a voltage of 100V. They exist in rotating and non-rotating
  variants. The overall intent is to encourage using them above the smaller
  panels, as server load is proportional to number of panels.
  
  We also replaced the solar panel icons. IMO the new ones should be easy to
  recognise.

- Changed the plate machine to take 1 ingot per plate, not 4.

  This is primarily because *that's what every single other mod does*. We added
  a config option in case you prefer the old behaviour, but why would you?
  
- Added a Sample and Hold chip.

  This has one analog and one digital input. It latches the analog input when
  given a signal on the digital input, outputting it until the next time it gets
  such a signal.
  
- Added recipes for the various analog chips.

  ...oops?
  
- The Auto-Miner can now output to any inventory, not just vanilla chests.

  Is this a bugfix or a feature?


### Known bugs

- The generators sometimes fail to pull very much power, preventing them from
  spinning up.
  
  We're on it. A workaround is to add a transformer between it and its power
  source, *set to "Isolated" mode*.
  
### Bugfixes

- Breaking the electrical power exporters by hand no longer destroys the block.

- Fixed a graphical glitch in the industrial data logger.

- Street lamps can no longer be placed on top of each other.

- Fixed a crash bug in the utility-pole code.

- Fixed a memory leak when Eln is run on (some) empty servers.

- Fixed the redstone-to-voltage converter not always updating when it should.

- Fixed thermal probes connecting to non-thermal cables.

- Fixed battery tooltip showing as J instead of kJ.

- Fixed middle-click not working for SixNode multiblocks.

- Also fixed WAILA not working properly, ditto.

- Steam turbines can now be built with iron instead of aluminum, if aluminum doesn't exist. This makes no sense.

- Machines now lose all progress if you change what they're doing. No more quick-compressed diamonds.

- Fixed fuses blowing simply from connecting them.

- Fixed NPE in the WAILA code for fire buzzers.

- Misc. typo fixes.

- Fixed the probable cause of Minecraft crashes sometimes destroying all placed Eln items.

  We still recommend taking backups, but I feel good about this one.

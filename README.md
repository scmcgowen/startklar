# Startklar

Startklar is a server-side Fabric mod for modern Minecraft versions that allows players to fly away without Elytra's
from the spawn.

When a player is jumping off of a cliff near the world spawn on the overworld, or press space twice, they automatically start flying. In this
flying mode, a player also does not need to worry about taking fall damage upon landing.

Ported to 1.20.1 by Herr Katze, along with some slight additons to its features

***

## Configuration

This mod comes with a TOML file on the server at `config/startklar/server.toml`, allowing to adjust a few aspects about
its behaviour. This is how it might look like:

```toml
# This message is being shown to the player when they have not used their boost yet midair.
# default: ᴘʀᴇss [sʜɪғᴛ] ғᴏʀ ᴀ ʙᴏᴏsᴛ!
boostIndicator = "ᴘʀᴇss [sʜɪғᴛ] ғᴏʀ ᴀ ʙᴏᴏsᴛ!"

# The spawn range where players can start flying, measured in blocks.
# This is a box with the world spawn at the center.
# default: 32
spawnDiameter = 32

# Determines after how many blocks of fall distance the fly mode should be auto-toggled.
# range: 0.0 - 256.0
# default: 3.0
toggleAfterFallDistanceOf = 3.0

# The flight duration of the boost. Setting it to 0 means the boost is disabled.
# range: 0 - 3
# default: 1
flightDuration = 1

# Whether or not the double tap to launch applies to creative players
# default: false
affectCreativePlayers = false

```

***

## Original by Sammy
The original project by Sammy can be found at
https://codeberg.org/SammyForReal/startklar

###### Copyright © 2025 Sammy L. Koch and Herr Katze

###### Startklar is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, version 3 only of the License.

###### Startklar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.

###### You should have received a copy of the GNU Lesser General Public License along with this mod. If not, see http://www.gnu.org/licenses/.

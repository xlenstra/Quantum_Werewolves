# Quantum Werewolves
This is a kotlin application used for managing and running a game of quantum werewolves.

Quantum Werewolves is game based on Werewolves of Miller's hallow. At the start of the game, a large number of worlds are generated, and for each world a different role division is created.
Players then preform their night actions in each world, which may render some worlds impossible. For example, the witch cannot poison a player that is already dead, and trying to do so will thus 'collapse' the world.
Additionally, some roles that receive information, such as the seer, may see information incompatible with some worlds, thus rendering those worlds impossible.

Keeping track of hundreds or thousands of worlds by hand is virtually undoable in general. Thus, I've created this application to do all the tedious work.
Despite this, there is still some manual labor involved in running the game. Most notably, votes for the lynch must still be tallied by hand, and only the result can be entered in to the application.
Only partly counting votes of dead players and double counting votes of the earlybird must thus be done manually.
Furthermore, inputting all the night actions of all players, especially for large groups at the start of the game, might prove to be tedious.

Lastly, due to localization, the names of all roles are currently shown in Dutch everywhere in the program. 
I'm planning to copy+paste the translation system of unciv to this application, but don't have the time for this as of yet.

A dutch version of the rules of an example game can be found here:
https://docs.google.com/document/d/1p-y27NLFZDsysQQsX-49uuhBoD239-hMBCSW4CjliAU/edit?usp=sharing


# Roles

Currently, the following roles are supported. In the future I might plan on making roles moddable, but I currently don't have the time for this.

## Team Villager

* Villager - No special powers.
* Villaging Villager - The percentage of each player being a villaging villager is published each morning and evening.
* Earlybird - If this player is the first to vote, all their votes count double.
* Fraudster - May choose once per game to have their vote count twice this day.
* Guardian Angel - May choose one player each nigh. They cannot be killed by the werewolves.
* Slut - May spend the night at another player. They cannot die when attacked, but will die when the player they sleep at is attacked.
* Casanova - May invite another player each night. This player cannot die when attacked, but will die when the casanova is attacked.
* Hamster - Turns into a hamster each night. Cannot be killed by the werewolves when attacked, but will die when they are seen by a seer.
* Hamster Maniac - May transform another player into a hamster each night. They retain their original powers, but gain those of a normal hamster in addition.
* Blessed - May bless another player each night. If they bless the werewolf that makes the kill this night, the kill will fail instead. If they bless a good player that is cursed, they are no longer cursed.
* Seer - May see another player each night. They learn the role of that player, and all worlds in which they are a seer and that player does not have that role become impossible.
* Old Seer - May see another player each night. They learn the team of that player, and all worlds in which they are an old seer and that player does not have that team become impossible.
* Apprentice Seer - When the (old) seer dies, the apprentice seer becomes a seer of the same type.
* Lycanthrope - Is seen as a werewolf by both the seer and old seer.
* Witch - May poison one player during the game. That player dies that night.
* Hunter - When the hunter dies, they shoot another player, who will also die. This applies to both dying at night as well as during the day.

## Team Werewolf

* Werewolf - The werewolves may kill a player each night.
* Cursed - The cursed may curse another player each night. Unless this player is a werewolf, their action will fail this night. If this player is blessed, they are no longer blessed.

## May change teams

* Small Wild One - Chooses an example on the first night. Belongs to the villager team while this example lives, but becomes a werewolf when they die.
* Devil - Chooses an example on the second night. If they are a seer, apprentice seer, old seer or werewolf, they become that role. Ohterwise, they become a villager.


# Unciv

Most of the UI code and a lot of the utility code in this code base is based on the game Unciv, which can be found here: www.github.com/yairm210/Unciv.

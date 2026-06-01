# Worlds Colliding - Core Mod
Worlds Colliding is the core foundation of the modpack of the same name. This mod serves as the technical engine, adding specific mechanics essential to the modpack’s narrative and progression system.

<ins>__Key Features__</ins>:

- Custom Story Dimension : Added a Story Dimension where all the story takes place. This dimensions ensures that every players can enjoy the story even in a multiplayer environment. 

- Narrative Engine: Implements a custom dialogue system with fading and image rendering.

## Community

Join our [discord](https://discord.gg/yMcCq7rWbs) !<br><br>
Share your ideas or bug discoveries with us !<br>
https://github.com/GCJOJO/WorldsColliding/issues <br>

## Developing with this mod
<ins>__How to build__</ins>:
1) Install Java JDK 17
2) Open command prompt or any IDE and run : 
 ```./gradlew build```
3) Enjoy !

<ins>__How to use the dialogue system__</ins>: <br>
First create a resource pack using this folder structure
```
    resourcepacks/
        my_resourcepack/
            assets/
                 lang/
                     en_us.json
                     any other language files...
                 dialogues.json
            pack.mcmeta
```

Inside dialogues.json define the different speakers that may speak in your dialogues.

```jsonc
{
  "speakers" : [
    // The speaker with id 0 is named Speaker 1 and its display color is white, this speaker doesn't play any sound when talking
    { "id" : 0, "name": "Speaker 1", "color": "FFFFFFFF", "sounds" : [] },
    // The speaker with id 1 is named Speaker 2 and its display color is red, this speaker plays the note block banjo sound when talking 
    { "id" : 1, "name": "Speaker 2", "color": "FFFF0000", "sounds" : ["block.note_block.banjo"] },
    // The speaker with id 2 is named Speaker 3 and randomly plays the sounds of the xylophone, the flute, the basedrum and the sound of falling on a mud block
    { "id" : 2, "name": "Speaker 3", "color": "FFFFFFFF", "sounds" : ["block.note_block.xylophone", "block.note_block.flute", "block.note_block.basedrum", "block.mud.fall"] },
    //...
  ]
}
```

Here is how you define a dialogue, there are multiple actions that can be defined as follows:
```jsonc
{
  "speakers" : [
      //...
  ],
  "dialogue_1" : [
      { "action" :  "message", "speaker" :  0, "text" :  "this action displays a message with speaker id 0"},
   
      { "action": "change_set", "set": "dialogue_2" },
   
      { "action": "choice", "option1": "Choose option 1 !", "action1": "action1_choosen", "save1": "option1", "option2": "Choose option 2 !", "action2": "action2_choosen", "save2": "option2" },
   
      // this actions displays a fading animation that takes 20 ticks and fades the screen from being visible to a white screen   
      { "action": "fade", "from": "00000000", "to": "FFFFFFFF", "time": 20 },
   
      { "action": "wait", "time": 20 },
   
      // clears every actions, 
      { "action": "clear" }, 
      //you may specify which action type to clear
      { "action": "clear", "cleared_action": "fade" }, 
   
      // displays an image on the screen, the width and height are the original size of the image, the image should be properly resized once shown to the player
      { "action": "image", "id": 0, "image": "my_resourcepack:textures/gui/my_cool_texture.png", "width": 64, "height": 64},
      
      // This actions move the image with id 0 with a duration of 80 ticks. 
      // The image starts at a position of 10% of the screen and ends at 90% of the screen and blends from 0% transparency to 100%
      { "action": "move_image", "id": 0, "start_x": 10, "start_y": 10, "end_x": 90, "end_y": 90, "alpha_start": 0, "alpha_end": 255, "time": 80 }
      
      // This actions executes a command as the player reading the dialogue. 
      // The <PLAYER> text will be replaced by the player's name.
      { "action": "command", "command": "say hello from <PLAYER> !" }
      
      // The command is execute as if the player typed it without needing permisions, you don't need to do /execute positioned as ... for any positioned command
      // You can simply do :
      { "action": "command", "command": "tp @s ~ ~10 ~" }
  ]
}
```
When defining text such as message or speaker names, you may use localized keys and translate them inside of `en_us.json` or any other language files.

You may display you dialogues using the following commands : <br>
``/dialogue set my_datapack:dialogue_1`` <br>
``/dialogue play``

<ins>__What are blocking and non-blocking actions ?__</ins><br>
Blocking actions are considered as actions that blocks the current dialogue, when they end, they wait for user input instead of directly going to the next action. <br>
There can be only one Blocking Action at any point.

Non-blocking actions do not wait user input before going to the next action. <br>
Non-blocking actions can be stacked on top of each others and stack on top of one Blocking Action and need to be cleared manually using the clear action.

Here is a table of Blocking and Non-Blocking Actions

|     Blocking     |  Non-Blocking   |
|:----------------:|:---------------:|
|  MessageAction   |   FadeAction    |
|   ChoiceAction   |   ImageAction   |
|    WaitAction    |   NextAction    |
|                  |  CreditAction   |
|                  |   ClearAction   |

_This project is currently in active development_

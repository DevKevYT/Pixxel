Hot to add Items:
    There are currently two item types: SWORD and HANDHELD
    HANDHELD does not essentially need an Animation (This is just a Sprite held in the player's hands)
    but there are still some rules to follow, because other than the SWORD, this Sprite needs to have images
    of all four player directions (SWORD only left and right).

    In the Items file, you don't need to specify the whole image path, instead the region names of the linked items.atlas file.

    So, to set up an Item that can get held in the player's hands, the first thing that needs to get registered in
    the "items.json" file is the Variable "useType: HANDHELD"
    The second thing to do is to add the Item (Animations) for all 4 directions:
    itemAnimation: [
        frameRate: 8
        ["torch-front"], ["torch-left"], ["torch-up"], ["torch-right"]
    ]
    The outer Arrays are the four directions of the player (0: front, 1: left, 2: up, 3: right), the inner arrays are the Item animation frames
    (Since ITEM does not really need an animation, usually the inner Arrays only have one entry. Of course you could do: ["torch-front1", "torch-front2" ...]

    SWORD is pretty similar, but here, the inner Arrays NEED to have multiple entries (4) since a sword attack has 4 player frames. Another difference is
    that here, you only need two outer arrays:

    useType: SWORD
    itemAnimation: [
        ["iron-sword-left-0", "iron-sword-left-1", "iron-sword-left-2", "iron-sword-left-3"], ["iron-sword-right-0", "iron-sword-right-1", "iron-sword-right-2", "iron-sword-right-3"]
    ]
    It is also important to notice, that "frameRate" does not need to get specified, because the player sprite Animation is locked at 10 Frames per Second.

    useType: BOW
    The first four arrays are like HANDHELD
    The last two are the bow tension - as usual left side first and then the right side

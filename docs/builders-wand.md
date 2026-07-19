Implement a builder's wand allowing the quick placement of many blocks. The purpose is to allow the placement of many like blocks more quickly to prevent repetitive building:

1. Pulls blocks from inventory
2. Recognizes the material of the block the player is looking at at outlines the suggested placement of more blocks based on the block they are looking at (and the blocks around it)
3. Recognize blocks that are aligned (looking at the left side of a 3 block pillar should render 3 blocks that could be placed on that side). If the user doesn't have enough blocks then the wand will let the player know when they try to place blocks (place no blocks)
4. Should support stair and slab orientations
5. Should render an "imaginary block" for each potential block with some degree of opacity to (so players can see what they are going to place before they do)
6. Left clicking cycles between the tier modes. Each tier mode allows a different number of blocks to be placed at once
    1. There should be a clear sound and message to the player that they are switching modes
7. 3 Tiers upgrading reach (while holding the wand) and number of blocks that can be placed at a time. The number of blocks able to placed are the tier modes. Reach doesn't change when player switches tier mode, just using the maximum reach always.
    1. Building lvl 32 - base reach, can place 5 blocks at a time
    2. Building lvl 48 - +2 reach, can place 11 blocks at a time
    3. Building lvl 55 - +4 reach, can place 21 blocks at a time

## More specification:

- Use ItemDisplay blocks and set them to glowing to indicate placement
- Looking at a flat wall of stone -> Fill the adjacent empty spaces in that plane
  Looking at a 1-block pillar -> Extend the pillar upward
  Looking at the edge of a wall (corner) -> Extend along the wall that the player has direct eye sight to
  Looking at the top of a floor -> Extend the floor upwards
  Looking at a single isolated block -> Just place 1 adjacent block on the face being looked at
  Looking at a diagonal line of blocks -> If they are all the same block, extend the line in the direction of the face the player is look at for all blocks (so not a straight line)
- The blocks able to be placed are based off of distance from the block the player is looking at (block being looked at is the middle)
- For stairs, they should match the directional orientation of the reference block, regardless of which side is being looked at
- Slabs should match top or bottom as anticipated
- The wand should stack reach with the ExtendedReach reward
- BlockReturn should work as normal
- Placing blocks with a wand should only count as 1 XP event
- The wand will have its own recipe: " R ", "NSN", " R " - N = netherite scrap, S = stick, R = Resin clump
- The upgrades will work automatically with the wand
- Only cycle on left click air
- Player's need to switch to a different block if they want to place a single block
- The ghost preview should show without clicking, as long as the player is holding the wand, the visualization will attempt to render
- Should pull from the inventory in slot order
- The wand should not pull from stacks that have custom NBT of any kind (including custom model data like what we use in the skills plugin)
- The wand should not consume anything in creative mode
- If there are blocks in the way of placement, that simply means that is not a valid spot to place a block
- Placing blocks needs to check grief prevention and world guard for the placement
- There should be a 10 tick cooldown in between placement (to prevent spam)

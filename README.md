# Update Notice

## v2.0

First, make the full backup file, unintall v1.x, then import backup file.
This version support audio and video in marks.

# About

In this app, a "slice" means item or card.

Common Role for this app:

- Flashcard
- Todo List
- Memo
- Notes
- Short wiki

## Quick Guide

Mainly Function List:

- Grouped slice
- Search, filter, sort by many ways
- Countdown with time step
- Hide or show your slice
- Export and import
- Bulk Operation on slices
- Show image
- Touch action (swipe, long click)
- Basically all text is selectable (by forbidden all touch action)
- Night Mode

### About how to show the image:

Each slice can show it's own image (just one image), the image's path is in your `Pictures/**/SliceNote/[group_name]/[slice_front].xxx`.

For instance:

1. be sure you granted  app's permission to read your storage, you can check it on menu.

2. you have a slice like below (note your marks cannot be empty):

```json
{"group": "insect", "front":"mosquito", "back":"", "marks": " ", ...}
```

3. prepare a image called "mosquito", jpg or png or any other image type is alright.

4. put this image to `Pictures/SliceNote/insect/`

5. the image will show when you click "Marks" in item's menu (right colored strip)


## Tips

1. click title will refresh list
2. long click title to show hidden list
3. export only apply on current group , if you want export all data, just click button "All" and then use menu's export


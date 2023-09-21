# About

In this app, a "slice" means item or card.

You can use this app for:

- Make Flashcard and Learn by Time Step.
- Make Todo List (Bujo)
- Take Quick Notes or Memo
- Record Atomic Idea (Zettelkasten)
- Organized Short wiki
- ...

# Quick Guide

Mainly Function List:

- Grouped slice
- Search, filter, sort by many ways
- Countdown with time step
- Hide or show your slice
- Export and import
- Bulk Operation on slices
- Show image, audio, video
- Touch action (swipe, long click)
- Basically all text is selectable (by forbidden all touch action)
- Night Mode

## How to use Seq

The true Seq times depend on countdown step, if countdown step set to 1 min,
then every minutes, each hide card whose Seq great than zero will subtract to 1.
But IT dose not subtract to 0, if card change it Seq change from 1 to 0, it'll
subtract to -1, not 0. Then the card will be unhide.

Let's make an instance:
1. you have a slice set to 10, and hide it.
2. you set the countdown step to 1 minutes in setting.
3. close the app and wait about 5 minutes.
4. open the app you can see the slice's seq are become 5.
5. close the app and wait about 5 minutes again.
6. open the app you will see the slice's seq become -1 and unhidden.


## How to show the image

Each slice can show it's own image (just one image), the image's path is in
your `Pictures/**/SliceNote/[group_name]/[slice_front].xxx`.

For instance:

1. be sure you granted app's permission to read your storage, you can check it on menu.

2. you have a slice like below (note your marks cannot be empty):

```json
[
  {
    "group": "insect", 
    "front":"mosquito",
    "back":"vampire",
    "marks": " ",
    "seq": 0,
    "prior": 0,
    "hide": false,
    "media": 1,
  }
]
```

3. prepare a image called "mosquito", jpg or png or any other image type is alright.

4. put this image to `Pictures/SliceNote/insect/` (media storage path).

5. the image will show when you click "Marks" in item's context menu.

NOTE: show audio or video are the same.

Some media storage path:

```
Images:
    DCIM/ Pictures/
Audios:
    Alarms/ Audiobooks/ Music/ Notifications/ Podcasts/ Ringtones/
Videos:
    DCIM/ Movies/ Pictures/
```

# Other Tips

1. click title will refresh list
2. long click title to show hidden list
3. export only apply on current group , if you want export all data,
just click button "All" and then use menu's export


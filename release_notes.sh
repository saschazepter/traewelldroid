#!/usr/bin/env bash

OUTPUT_FILE="${RUNNER_TEMP:-.}/RELEASE_NOTES.md"

GITMOJI_REPLACEMENTS=(
  'art=🎨'
  'zap=⚡️'
  'fire=🔥'
  'bug=🐛'
  'ambulance=🚑️'
  'sparkles=✨'
  'memo=📝'
  'rocket=🚀'
  'lipstick=💄'
  'tada=🎉'
  'white_check_mark=✅'
  'lock=🔒️'
  'closed_lock_with_key=🔐'
  'bookmark=🔖'
  'rotating_light=🚨'
  'construction=🚧'
  'green_heart=💚'
  'arrow_down=⬇️'
  'arrow_up=⬆️'
  'pushpin=📌'
  'construction_worker=👷'
  'chart_with_upwards_trend=📈'
  'recycle=♻️'
  'heavy_plus_sign=➕'
  'heavy_minus_sign=➖'
  'wrench=🔧'
  'hammer=🔨'
  'globe_with_meridians=🌐'
  'pencil2=✏️'
  'poop=💩'
  'rewind=⏪️'
  'twisted_rightwards_arrows=🔀'
  'package=📦️'
  'alien=👽️'
  'truck=🚚'
  'page_facing_up=📄'
  'boom=💥'
  'bento=🍱'
  'wheelchair=♿️'
  'bulb=💡'
  'beers=🍻'
  'speech_balloon=💬'
  'card_file_box=🗃️'
  'loud_sound=🔊'
  'mute=🔇'
  'busts_in_silhouette=👥'
  'children_crossing=🚸'
  'building_construction=🏗️'
  'iphone=📱'
  'clown_face=🤡'
  'egg=🥚'
  'see_no_evil=🙈'
  'camera_flash=📸'
  'alembic=⚗️'
  'mag=🔍️'
  'label=🏷️'
  'seedling=🌱'
  'triangular_flag_on_post=🚩'
  'goal_net=🥅'
  'dizzy=💫'
  'wastebasket=🗑️'
  'passport_control=🛂'
  'adhesive_bandage=🩹'
  'monocle_face=🧐'
  'coffin=⚰️'
  'test_tube=🧪'
  'necktie=👔'
  'stethoscope=🩺'
  'bricks=🧱'
  'technologist=🧑‍💻'
  'money_with_wings=💸'
  'thread=🧵'
  'safety_vest=🦺'
  'airplane=✈️'
  't-rex=🦖'
)

replace_gitmojis() {
  local line
  local replacement
  local code
  local emoji

  while IFS= read -r line || [ -n "$line" ]; do
    for replacement in "${GITMOJI_REPLACEMENTS[@]}"; do
      code=${replacement%%=*}
      emoji=${replacement#*=}
      line="${line//:${code}:/${emoji}}"
    done

    printf '%s\n' "$line"
  done
}

write_release_log() {
  if [ -z "$PREVIOUS_TAG" ]; then
    git log "$LATEST_TAG" --reverse --pretty=format:"- %s"
  else
    git log "${PREVIOUS_TAG}..${LATEST_TAG}" --reverse --pretty=format:"- %s"
  fi
}

LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null)

if [ -z "$LATEST_TAG" ]; then
  echo "Error: Could not find any tag"
  exit 1
fi

PREVIOUS_TAG=$(git describe --tags --abbrev=0 "${LATEST_TAG}^" 2>/dev/null)

echo "# What's changed in ${LATEST_TAG}" > "$OUTPUT_FILE"

echo "" >> "$OUTPUT_FILE"

write_release_log | replace_gitmojis >> "$OUTPUT_FILE"

echo "" >> "$OUTPUT_FILE"

echo "Release Notes created."

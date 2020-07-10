
NEW_VERSION=$1
NEXT_SNAPSHOT=$2

git checkout master
git pull

echo "version in ThisBuild := \"$NEW_VERSION\"" > version.sbt
jq --arg version "$NEW_VERSION" '. + {"version": $version}' package.json > package-tmp.json
mv package-tmp.json package.json
git add version.sbt
git commit -m "Bump version to $NEW_VERSION"
git tag -a "$NEW_VERSION" -m "Release $NEW_VERSION"

echo "version in ThisBuild := \"$NEXT_SNAPSHOT\"" > version.sbt
jq --arg version "$NEXT_SNAPSHOT" '. + {"version": $version}' package.json > package-tmp.json
mv package-tmp.json package.json
git add version.sbt
git commit -m "Bump version to $NEXT_SNAPSHOT"

git push origin HEAD --tags


NEW_VERSION=$1
NEXT_SNAPSHOT=$1

git checkout master
git pull

echo "version in ThisBuild := $NEW_VERSION" > version.sbt
git add version.sbt
git commit -m "Bump version to $NEW_VERSION"
git tag -a "$NEW_VERSION" -m "Release $NEW_VERSION"

echo "version in ThisBuild := $NEXT_SNAPSHOT" > version.sbt
git add version.sbt
git commit -m "Bump version to $NEXT_SNAPSHOT"

git push origin HEAD --tags

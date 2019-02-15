#!/bin/bash

# Copied from & adapted based on https://github.com/spring-cloud/spring-cloud-build/blob/master/docs/src/main/asciidoc/ghpages.sh

set -e

# Set default props like MAVEN_PATH, ROOT_FOLDER etc.
function set_default_props() {
    # The script should be executed from the root folder
    ROOT_FOLDER=`pwd`
    echo "Current folder is ${ROOT_FOLDER}"

    if [[ ! -e "${ROOT_FOLDER}/.git" ]]; then
        echo "You're not in the root folder of the project!"
        exit 1
    fi

    # Prop that will let commit the changes
    COMMIT_CHANGES="no"
    MAVEN_PATH=${MAVEN_PATH:-}
    if [ -e "${ROOT_FOLDER}/mvnw" ]; then
        MAVEN_EXEC="$ROOT_FOLDER/mvnw"
    else
        MAVEN_EXEC="${MAVEN_PATH}mvn"
    fi
    echo "Path to Maven is [${MAVEN_EXEC}]"
    if [ -z $REPO_NAME ]; then
        REPO_NAME=$(git remote -v | grep origin | head -1 | sed -e 's!.*/!!' -e 's/ .*//' -e 's/\.git.*//')
    fi
    echo "Repo name is [${REPO_NAME}]"
}

# Adds the oauth token if present to the remote url
function add_oauth_token_to_remote_url() {
    remote=`git config remote.origin.url | sed -e 's/^git:/https:/'`
    echo "Current remote [${remote}]"
    if [[ "${RELEASER_GIT_OAUTH_TOKEN}" != "" && ${remote} != *"@"* ]]; then
        echo "OAuth token found. Will reuse it to push the code"
        withToken=${remote/https:\/\//https://${RELEASER_GIT_OAUTH_TOKEN}@}
        git remote set-url --push origin "${withToken}"
    else
        echo "No OAuth token found"
        git remote set-url --push origin `git config remote.origin.url | sed -e 's/^git:/https:/'`
    fi
}

# Check if gh-pages exists and docs have been built
function check_if_anything_to_sync() {
    add_oauth_token_to_remote_url
    if ! (git remote set-branches --add origin gh-pages && git fetch -q) ; then
        echo "No gh-pages, so not syncing"
        exit 0
    fi

    if ! [ -d docs/target/generated-docs ] && ! [ "${BUILD}" == "yes" ]; then
        echo "No gh-pages sources in docs/target/generated-docs, so not syncing"
        exit 0
    fi
}

function retrieve_current_branch() {
    # Code getting the name of the current branch. For master we want to publish as we did until now
    # http://stackoverflow.com/questions/1593051/how-to-programmatically-determine-the-current-checked-out-git-branch
    # If there is a branch already passed will reuse it - otherwise will try to find it
    CURRENT_BRANCH=${BRANCH}
    if [[ -z "${CURRENT_BRANCH}" ]] ; then
      CURRENT_BRANCH=$(git symbolic-ref -q HEAD)
      CURRENT_BRANCH=${CURRENT_BRANCH##refs/heads/}
      CURRENT_BRANCH=${CURRENT_BRANCH:-HEAD}
    fi
    echo "Current branch is [${CURRENT_BRANCH}]"
    git checkout ${CURRENT_BRANCH} || echo "Failed to check the branch... continuing with the script"
}

# Switches to the provided value of the release version. We always prefix it with `v`
function switch_to_tag() {
    git checkout v${VERSION}
}

# Build the docs if switch is on
function build_docs_if_applicable() {
    if [[ "${BUILD}" == "yes" ]] ; then
        ./mvnw clean install -P docs -pl docs -DskipTests
    fi
}

# Get whitelisted branches - assumes that a `docs` module is available under `docs` profile
function retrieve_doc_properties() {
    WHITELIST_PROPERTY=${WHITELIST_PROPERTY:-"docs.whitelisted.branches"}
    WHITELISTED_BRANCHES_VALUE=$("${MAVEN_EXEC}" -q \
        -Dexec.executable="echo" \
        -Dexec.args="\${${WHITELIST_PROPERTY}}" \
        org.codehaus.mojo:exec-maven-plugin:1.3.1:exec \
        -P docs \
        -pl docs | tail -1 )
    echo "Extracted '${WHITELIST_PROPERTY}' from Maven build [${WHITELISTED_BRANCHES_VALUE}]"
}

# Stash any outstanding changes
function stash_changes() {
    git diff-index --quiet HEAD && dirty=$? || (echo "Failed to check if the current repo is dirty. Assuming that it is." && dirty="1")
    if [ "$dirty" != "0" ]; then git stash; fi
}

# Switch to gh-pages branch to sync it with current branch
function add_docs_from_target() {
    local DESTINATION_REPO_FOLDER=${ROOT_FOLDER}
    cd ${DESTINATION_REPO_FOLDER}
    git checkout gh-pages
    git pull origin gh-pages

    # Add git branches
    ###################################################################
    if [[ -z "${VERSION}" ]] ; then
        copy_docs_for_current_version
    else
        copy_docs_for_provided_version
    fi
    commit_changes_if_applicable
}


# Copies the docs by using the retrieved properties from Maven build
function copy_docs_for_current_version() {
    if [[ "${CURRENT_BRANCH}" == "master" ]] ; then
        echo -e "Current branch is master - will copy the current docs only to the root folder"
        for f in docs/target/generated-docs/*; do
            file=${f#docs/target/generated-docs/*}
            if ! git ls-files -i -o --exclude-standard --directory | grep -q ^$file$; then
                # Not ignored...
                cp -rf $f ${ROOT_FOLDER}/
                git add -A ${ROOT_FOLDER}/$file
            fi
        done
        COMMIT_CHANGES="yes"
    else
        echo -e "Current branch is [${CURRENT_BRANCH}]"
        # http://stackoverflow.com/questions/29300806/a-bash-script-to-check-if-a-string-is-present-in-a-comma-separated-list-of-strin
        if [[ ",${WHITELISTED_BRANCHES_VALUE}," = *",${CURRENT_BRANCH},"* ]] ; then
            mkdir -p ${ROOT_FOLDER}/${CURRENT_BRANCH}
            echo -e "Branch [${CURRENT_BRANCH}] is whitelisted! Will copy the current docs to the [${CURRENT_BRANCH}] folder"
            for f in docs/target/generated-docs/*; do
                file=${f#docs/target/generated-docs/*}
                if ! git ls-files -i -o --exclude-standard --directory | grep -q ^$file$; then
                    cp -rf $f ${ROOT_FOLDER}/${CURRENT_BRANCH}
                    git add -A ${ROOT_FOLDER}/${CURRENT_BRANCH}/$file
                fi
            done
            COMMIT_CHANGES="yes"
        else
            echo -e "Branch [${CURRENT_BRANCH}] is not on the white list! Check out the Maven [${WHITELIST_PROPERTY}] property in
             [docs] module available under [docs] profile. Won't commit any changes to gh-pages for this branch."
        fi
    fi
}

# Copies the docs by using the explicitly provided version
function copy_docs_for_provided_version() {
    local FOLDER=${DESTINATION_REPO_FOLDER}/${VERSION}
    mkdir -p ${FOLDER}
    echo -e "Current tag is [v${VERSION}] Will copy the current docs to the [${FOLDER}] folder"
    for f in ${ROOT_FOLDER}/docs/target/generated-docs/*; do
        file=${f#${ROOT_FOLDER}/docs/target/generated-docs/*}
        copy_docs_for_branch ${file} ${FOLDER}
    done
    COMMIT_CHANGES="yes"
    CURRENT_BRANCH="v${VERSION}"
}

# Copies the docs from target to the provided destination
# Params:
# $1 - file from target
# $2 - destination to which copy the files
function copy_docs_for_branch() {
    local file=$1
    local destination=$2
    if ! git ls-files -i -o --exclude-standard --directory | grep -q ^${file}$; then
        cp -rf $f ${destination}
        git add -A ${destination}/$file
    fi
}

function commit_changes_if_applicable() {
    if [[ "${COMMIT_CHANGES}" == "yes" ]] ; then
        COMMIT_SUCCESSFUL="no"
        git commit -a -m "Sync docs from ${CURRENT_BRANCH} to gh-pages" && COMMIT_SUCCESSFUL="yes" || echo "Failed to commit changes"

        # Uncomment the following push if you want to auto push to
        # the gh-pages branch whenever you commit to master locally.
        # This is a little extreme. Use with care!
        ###################################################################
        if [[ "${COMMIT_SUCCESSFUL}" == "yes" ]] ; then
            add_oauth_token_to_remote_url
            echo "git push origin gh-pages" #git push origin gh-pages
        fi
    fi
}

# Switch back to the previous branch and exit block
function checkout_previous_branch() {
    # If -version was provided we need to come back to root project
    cd ${ROOT_FOLDER}
    git checkout ${CURRENT_BRANCH} || echo "Failed to check the branch... continuing with the script"
    if [ "$dirty" != "0" ]; then git stash pop; fi
    exit 0
}

# Prints the usage
function print_usage() {
cat <<EOF
The idea of this script is to update gh-pages branch with the generated docs. Without any options
the script will work in the following manner:

- if there's no gh-pages / target for docs module then the script ends
- for master branch the generated docs are copied to the root of gh-pages branch
- for any other branch (if that branch is whitelisted) a subfolder with branch name is created
    and docs are copied there
- if the version switch is passed (-v) then a tag with (v) prefix will be retrieved and a folder
    with that version number will be created in the gh-pages branch. WARNING! No whitelist verification will take place

USAGE:

You can use the following options:

-v|--version        - the script will apply the whole procedure for a particular library version
-b|--build          - will run the standard build process after checking out the branch

EOF
}


# ==========================================
#    ____   ____ _____  _____ _____ _______
#  / ____|/ ____|  __ \|_   _|  __ \__   __|
# | (___ | |    | |__) | | | | |__) | | |
#  \___ \| |    |  _  /  | | |  ___/  | |
#  ____) | |____| | \ \ _| |_| |      | |
# |_____/ \_____|_|  \_\_____|_|      |_|
#
# ==========================================

while [[ $# > 0 ]]
do
key="$1"
case ${key} in
    -v|--version)
    VERSION="$2"
    shift # passed argument
    ;;
    -b|--build)
    BUILD="yes"
    ;;
    -h|--help)
    print_usage
    exit 0
    ;;
    *)
    echo "Invalid option: [$1]"
    print_usage
    exit 1
    ;;
esac
shift # passed argument or value
done

set_default_props
check_if_anything_to_sync
if [[ -z "${VERSION}" ]] ; then
    retrieve_current_branch
else
    switch_to_tag
fi
build_docs_if_applicable
retrieve_doc_properties
stash_changes
add_docs_from_target
checkout_previous_branch
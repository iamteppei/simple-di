# How to publish to maven central

Step to generate signing key and setup Github Action pipeline.

## Step 1: generate the keypair 

```bash
gpg --full-generate-key
```

Select the following configuration prompts:

- Kind of key: 1 (RSA and RSA - default).
- Keysize: 4096 bits for optimal security.
- Validity period: 0 (key does not expire) or choose an explicit timeline.
- Real name & Email: Enter your name and the verified email associated with your GitHub/Sonatype accounts.
- Passphrase: Set a secure passphrase. Remember this, you will need to add it to Gradle.

## Step 2: Locate your key id

```bash
gpg --list-secret-keys --keyid-format=long
```

Look for the line beginning with sec. Your Key ID is the string right after the slash. Example

```bash
sec   rsa4096/3AA5C34371567BD2 2026-05-26 [SC]
```

## Step 2: Distribute Your Public Key

Maven Central needs to verify your signatures. Send your public key to an authorized keyserver (like Ubuntu) so the portal can check it:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

## Step 3: Export the Private Key for Gradle

```bash
gpg --export-secret-keys --armor YOUR_KEY_ID
```

Copy the full text block output, including the headers.

## Step 4: Update Your Credentials or CI/CD pipeline

Do this step when need to verify locally

```bash
# ~/.gradle/gradle.properties

# Sonatype API Tokens
mavenCentralUsername=your_sonatype_token_username
mavenCentralPassword=your_sonatype_token_password

# GPG Signing Setup
signing.keyId=YOUR_SHORT_KEY_ID
signing.password=YOUR_GPG_PASSPHRASE
signing.secretKeyRingFile=/Users/yourusername/.gnupg/secring.gpg
```

Or configure github action pipeline

```yml
- name: Build and Publish Release
run: ./gradlew publishAndReleaseToMavenCentral
env:
    ORG_GRADLE_PROJECT_mavenCentralUsername: ${{ secrets.SONATYPE_USERNAME }}
    ORG_GRADLE_PROJECT_mavenCentralPassword: ${{ secrets.SONATYPE_PASSWORD }}
    ORG_GRADLE_PROJECT_signingInMemoryKey: ${{ secrets.GPG_SIGNING_KEY }}
    ORG_GRADLE_PROJECT_signingInMemoryKeyPassword: ${{ secrets.GPG_PASSPHRASE }}
```          
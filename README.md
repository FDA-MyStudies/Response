# LabKey Response Module - Implements the Response Server Functionality of the MyStudies System

This LabKey module provides the Response Server functionality and services, including:

- Processing and storing all mobile app survey and active task responses
- Issuing enrollment tokens to research organizations
- Enrolling and unenrolling participants
- Creating database schemas that match each study's design and updating them as studies change
- Limited querying of data by the mobile app
- Providing web analytics, querying, reporting, and visualizations through manual and programmatic methods

## Setup Instructions

You can build and deploy this module to a standard LabKey Server deployment; see the
"Developer Setup Instructions" below for more details.

If you prefer, you can create a standalone distribution of the MyStudies Response server using the following steps.
_(Commands and paths are relative to your `Response` enlistment)_

1. Install JDK 17
   - Download and install the latest [Eclipse Temurin™ JDK 17](https://adoptium.net/releases.html?variant=openjdk17&amp;jvmVariant=hotspot)
   - Point your `JAVA_HOME` environment variable at this JDK 17 installation
1. Create 'Response' LabKey distribution
   - (Linux/MacOS) `./gradlew -I init.gradle -PdeployMode=prod :distributions:fda:distribution`
   - (Windows) `.\gradlew -I init.gradle -PdeployMode=prod :distributions:fda:distribution`
1. Locate distribution archive
   - (Linux/MacOS) `dist/response/LabKey*-response.tar.gz`
   - (Windows) `dist\response\LabKey*-response.tar.gz`
1. Follow [instructions for manual deployment](https://www.labkey.org/Documentation/22.7/wiki-page.view?name=manualInstall) of the distribution archive

## Developer Setup Instructions

This module can be developed within the LabKey Server platform (version 22.7.x). To setup a development environment for the Response Server (i.e. a standard LabKey Server distribution plus the Response module), follow these steps:

1. Checkout the LabKey Server 22.7.x public GitHub repositories: [Set Up a Development Machine](https://www.labkey.org/Documentation/22.7/wiki-page.view?name=devMachine)

1. Clone the Response module (this repository) into `server/modules`

1. Navigate to the root of your LabKey enlistment.

1. Append these two lines to the end of `settings.gradle`:
   ```
   include ":server:modules:Response"
   include ":server:modules:Response:distributions:fda"
   ```

1. On the command line (again, in the root of your working copy), run one of these commands (use the first command on Linux/OSX and the second on Windows):

    ```
    ./gradlew :server:modules:Response:distributions:fda:dist
    gradlew :server:modules:Response:distributions:fda:dist
    ```

1. [Build and deploy LabKey](https://www.labkey.org/Documentation/22.7/wiki-page.view?name=buildLabKey) with the Response module.

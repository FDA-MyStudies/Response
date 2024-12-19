# LabKey Response Module - Implements the Response Server Functionality of the MyStudies System

Warning: Do NOT use the `develop` branch of this repository! It is not maintained, so it no longer builds or runs
against the LabKey Server `develop` branch. Use the `release24.11-SNAPSHOT` branches of Response, UserReg-WS, and the
LabKey Server repositories instead.

Note: The FDA MyStudies project ended December 31, 2024. At that time, the `release24.11-SNAPSHOT` branch of this
repository was tested and deployed successfully using LabKey Server v24.11. The code in that branch is available
for use by any interested party, subject to the license detailed in the accompanying LICENSE.txt file, however, the
code is NOT maintained. Use at your own risk.

This LabKey module provides the Response Server functionality and services, including:

- Processing and storing all mobile app survey and active task responses
- Issuing enrollment tokens to research organizations
- Enrolling and unenrolling participants
- Creating database schemas that match each study's design and updating them as studies change
- Limited querying of data by the mobile app
- Providing web analytics, querying, reporting, and visualizations through manual and programmatic methods

## Setup Instructions

This module can be developed within the LabKey Server platform (version 24.11.x). To set up a development environment for
the Response Server (i.e. a standard LabKey Server distribution plus the Response module), follow these steps:

1. Checkout the LabKey Server 24.11.x public GitHub repositories: [Set Up a Development Machine](https://www.labkey.org/Documentation/24.11/wiki-page.view?name=devMachine)

1. Clone the Response module (this repository) into `server/modules`

1. Navigate to the root of your LabKey enlistment.

1. Append these two lines to the end of `settings.gradle`:
   ```
   include ":server:modules:Response"
   include ":server:modules:Response:distributions:response"
   ```

1. On the command line (again, in the root of your working copy), run one of these commands (use the first command on 
Linux/OSX and the second on Windows):

    ```
    ./gradlew :server:modules:Response:distributions:response:dist
    gradlew :server:modules:Response:distributions:response:dist
    ```

1. [Build and deploy LabKey](https://www.labkey.org/Documentation/24.11/wiki-page.view?name=buildLabKey) with the Response module.

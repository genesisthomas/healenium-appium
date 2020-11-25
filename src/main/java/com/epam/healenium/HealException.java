/**
 * Healenium-appium Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium;

public class HealException extends RuntimeException {

    public HealException() {
        super();
    }

    public HealException(String message) {
        super(message);
    }

    public HealException(String message, Throwable cause) {
        super(message, cause);
    }

    public HealException(Throwable cause) {
        super(cause);
    }
}

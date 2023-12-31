/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhongxj.providerpaging;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * The contract for the {@link ImageProvider}.
 */
class ImageContract {

    static final String AUTHORITY = "com.zhongxj.providerpaging.contentproviderpaging.documents";

    static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/images");

    interface Columns extends BaseColumns {

        String DISPLAY_NAME = "display_name";

        String ABSOLUTE_PATH = "absolute_path";

        String SIZE = "size";
    }

    static final String[] PROJECTION_ALL = new String[]{
            Columns._ID,
            Columns.DISPLAY_NAME,
            Columns.ABSOLUTE_PATH,
            Columns.SIZE
    };
}

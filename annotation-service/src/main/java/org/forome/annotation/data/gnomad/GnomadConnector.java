/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.data.gnomad;

import org.forome.annotation.data.gnomad.struct.GnomadResult;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.SourceMetadata;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GnomadConnector extends AutoCloseable {

    CompletableFuture<GnomadResult> request(Chromosome chromosome, long position, String reference, String alternative);

    List<SourceMetadata> getSourceMetadata();

    void close();
}
